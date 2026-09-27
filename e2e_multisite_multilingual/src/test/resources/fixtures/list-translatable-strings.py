import json, sys
def unesc(v):
    out=[];i=0
    while i<len(v):
        c=v[i]
        if c=='\\' and i+1<len(v):
            n=v[i+1]
            out.append({'|':'|','\\':'\\','n':'\n','r':'\r'}.get(n,n)); i+=2
        else:
            out.append(c); i+=1
    return ''.join(out)
def split(line):
    f=[];cur=[];i=0
    while i<len(line):
        c=line[i]
        if c=='\\' and i+1<len(line): cur.append(c); cur.append(line[i+1]); i+=2; continue
        if c=='|': f.append(unesc(''.join(cur))); cur=[]; i+=1; continue
        cur.append(c); i+=1
    f.append(unesc(''.join(cur)))
    return f
rows={}
for line in open(sys.argv[1]):
    line=line.rstrip('\n')
    if not line or line.startswith('#'): continue
    t,rest=line.split(': ',1)
    rows.setdefault(t,[]).append(split(rest))
items=[]
s=rows['surveys'][0]
items.append(('surveys','title',s[4]))
items.append(('surveys','description',s[5]))
for r in rows['steps']:
    items.append(('steps','name',r[3])); items.append(('steps','description',r[5]))
for r in rows['sections']:
    items.append(('sections','name',r[3])); items.append(('sections','description',r[5]))
for r in rows['questions']:
    for field,idx in (('text',3),('short_text',4),('tool_tip',5),('validation_text',9),('placeholder',12)):
        items.append(('questions',field,r[idx]))
for r in rows['select_items']:
    items.append(('select_items','display_text',r[3]))
seen=[]
for et,f,txt in items:
    if not txt or not txt.strip(): continue
    k=f"{et}|{f}|{txt}"
    if k not in seen: seen.append(k)
print(json.dumps(seen, ensure_ascii=False, indent=1))
