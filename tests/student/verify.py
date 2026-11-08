"""Run after python tools/run.py --setup, from the repository root."""
import functools
import itertools
import json
import os
import random
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'tools'))
from public_validate import check

def execute(requests):
    run = subprocess.run([os.environ.get('JAVA', 'java'), '-Xmx384m', '-cp',
                          str(ROOT / 'Java/build'), 'Main'],
                         input=''.join(json.dumps(x) + '\n' for x in requests),
                         encoding='utf-8', capture_output=True, check=True, timeout=20)
    answers = [json.loads(line) for line in run.stdout.splitlines()]
    assert len(answers) == len(requests)
    return answers

def positions(text, pattern):
    return [i for i in range(len(text) + 1) if text.startswith(pattern, i)]

@functools.lru_cache(None)
def merge_cost(blocks):
    if len(blocks) < 2:
        return 0
    return min(a+b+merge_cost(blocks[:i]+(a+b,)+blocks[i+2:])
               for i, (a, b) in enumerate(zip(blocks, blocks[1:])))

def oracle(x):
    task = x['task']
    if task == 'lcs':
        a, b = x['a'], x['b']
        for k in range(len(a), -1, -1):
            for chars in itertools.combinations(a, k):
                it = iter(b)
                if all(c in it for c in chars):
                    return {'length': k}
    if task == 'merge':
        return {'cost': merge_cost(tuple(x['sizes']))}
    if task == 'tree':
        def value(mask):
            return sum(w for i, w in enumerate(x['weights']) if mask >> i & 1)
        return {'weight': max(value(mask) for mask in range(1 << x['n'])
                              if all(not (mask >> u & mask >> v & 1) for u, v in x['edges']))}
    if task in ('naive', 'kmp', 'rk'):
        return {'positions': positions(x['text'], x['pattern'])}
    if task == 'aho':
        return {'counts': [[len(positions(t, p)) for p in x['patterns']] for t in x['texts']]}
    text = x['text']
    sa = sorted(range(len(text)), key=lambda i: text[i:])
    lcp = [0] * len(text)
    longest = ''
    for i in range(1, len(sa)):
        for a, b in zip(text[sa[i-1]:], text[sa[i]:]):
            if a != b:
                break
            lcp[i] += 1
        if lcp[i] > len(longest):
            longest = text[sa[i]:sa[i]+lcp[i]]
    return {'sa': sa, 'lcp': lcp, 'repeated': longest}

def tree(weights, edges):
    return dict(task='tree', n=len(weights), weights=weights, edges=edges)

small = [dict(task='lcs', a=a, b=b) for a,b in
         [('', ''),('', 'x'),('x', ''),('abc','bac'),('aa! a','a a'),('aXbYc','abc')]]
small += [dict(task='merge', sizes=v) for v in [[],[7],[0,0,0],[1,100,1],[10**9]*4]]
small += [tree([],[]),tree([-9],[]),tree([0],[]),tree([-3,-4],[[0,1]]),tree([7,7],[[0,1]])]
for text, pattern in [('',''),('','x'),('xy','xyz'),('aaa',''),('aaaaa','aaa'),
                      ('dj ab!dj ab','ab'),('Ab aB! Ab','Ab'),('~~~~~  ','~ ')]:
    small += [dict(task=t,text=text,pattern=pattern) for t in ['naive','kmp','rk']]
small += [dict(task='suffix',text=s) for s in ['', 'x','zyx','aaaa','banana','abXabYcdZcd']]
small += [dict(task='aho',patterns=p,texts=t) for p,t in
          [([],['','a']),(['a'],[]),(['he','she','he','e'],['shehe','','shehe']),
           (['a','aa','a'],['aaaa','a','aaaa'])]]
fixtures = list(small)
rng = random.Random(2515)
for _ in range(35):
    def word(n):
        return ''.join(rng.choice('ab !') for _ in range(n))
    a,b = word(rng.randrange(9)),word(rng.randrange(9))
    n = rng.randrange(9)
    small.extend([dict(task='lcs',a=a,b=b),dict(task='merge',sizes=[rng.randrange(5) for _ in range(n)]),
                  tree([rng.randrange(-3,5) for _ in range(n)],[[rng.randrange(i),i] for i in range(1,n)]),
                  dict(task='suffix',text=a),dict(task='aho',patterns=[a or 'a',b or 'b','a'],texts=[a+b,a,b])])
    small.extend(dict(task=t,text=a,pattern=b) for t in ['naive','kmp','rk'])
expected = [oracle(x) for x in small]
batch = small + small[::-1]
answers = execute(batch)
for i,(x,y,e) in enumerate(zip(batch,answers,expected+expected[::-1])):
    assert check(x,y,e),(i,x,y,e)
(HERE/'examples.jsonl').write_text(''.join(json.dumps(x)+'\n' for x in fixtures),encoding='utf-8')
(HERE/'examples-results.jsonl').write_text(''.join(json.dumps({'expected':e,'actual':y})+'\n'
    for e,y in zip(expected[:len(fixtures)],answers)),encoding='utf-8')
print(f'PASS {len(fixtures)} explicit fixtures, 280 randomized requests, {len(batch)} mixed requests including reset')

# Check both accepted optimal witnesses and malformed/non-optimal alternatives.
for x,good,bad in [
    (dict(task='lcs',a='ab',b='ba'),[dict(length=1,sequence=s) for s in 'ab'],dict(length=2,sequence='ab')),
    (dict(task='merge',sizes=[1,1,1]),[dict(cost=5,plan=p) for p in
     [[[0,0,1],[0,1,2]],[[1,1,2],[0,0,2]]]],dict(cost=5,plan=[[0,1,2],[0,0,1]])),
    (tree([1,1],[[0,1]]),[dict(weight=1,selected=[i]) for i in (0,1)],dict(weight=1,selected=[0,1])),
    (dict(task='suffix',text='abXabYcdZcd'),[],{})]:
    e = oracle(x)
    if x['task']=='suffix':
        good=[dict(e,repeated=s) for s in ['ab','cd']]
        bad=dict(e,repeated='zz')
    assert all(check(x,y,e) for y in good) and not check(x,bad,e)
print('PASS alternative optima and invalid witnesses for LCS, merge, tree and suffix')

def large(x,e,label):
    actual, = execute([x])
    assert check(x,actual,e),label
    print('PASS',label)

large(dict(task='lcs',a='ab'*500,b='ab'*500),{'length':1000},'LCS 1000 x 1000')
# Equal weights: an optimal binary merge tree has 56 leaves at depth 7 and 144 at depth 8.
large(dict(task='merge',sizes=[10**9]*200),{'cost':(56*7+144*8)*10**9},'merge 200 blocks, 64-bit total')
n=100000
large(tree([10**9]*n,[[i,i+1] for i in range(n-1)]),{'weight':(n//2)*10**9},'tree chain of 100000')
large(dict(task='naive',text='a'*3000,pattern='a'*3000),{'positions':[0]},'naive max dimensions')
large(dict(task='kmp',text='a'*200000,pattern='a'*100000),{'positions':list(range(100001))},'KMP max dimensions')
large(dict(task='rk',text='a'*10000,pattern='a'*2000),{'positions':list(range(8001))},'RK max product')
large(dict(task='rk',text='x',pattern='x'*10000),{'positions':[]},'RK max pattern length')
s='a'*50000
y,=execute([dict(task='suffix',text=s)])
assert y==dict(sa=list(reversed(range(len(s)))),lcp=list(range(len(s))),repeated=s[1:])
print('PASS suffix 50000 equal characters, full SA, LCP and longest repeat')
large(dict(task='aho',patterns=['a'*100000],texts=['a'*200000]),{'counts':[[100001]]},'Aho deep trie and maximum text')
large(dict(task='aho',patterns=['a'*10]*10000,texts=['a'*2000]*100),
      {'counts':[[1991]*10000]*100},'Aho 10000 patterns, 100 texts and 1000000 counts')
