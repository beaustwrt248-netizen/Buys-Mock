#!/usr/bin/env python3
"""Side-effect-free integrity checks for Australian device catalogue candidates."""
from __future__ import annotations
import argparse, json, re, sys
from pathlib import Path
from typing import Any

CATEGORIES={"mobile_phone","tablet","laptop","desktop","console","wearable"}
CARRIERS={"telstra","optus","vodafone","boost","amaysim","belong"}
STORAGE=re.compile(r"^\s*\d+(?:\.\d+)?\s*(?:GB|TB)\s*$",re.I)

def text(v:Any)->str: return str(v or "").strip()
def issues(row:dict[str,Any])->list[dict[str,str]]:
    out=[]; cat=text(row.get("category")); brand=text(row.get("brand")); model=text(row.get("model_name")); number=text(row.get("model_number")); storage=row.get("storage_options")
    def add(code,field,msg): out.append({"code":code,"field":field,"message":msg})
    if cat not in CATEGORIES: add("invalid_category","category","Unsupported canonical category")
    if not brand: add("missing_brand","brand","Manufacturer brand is required")
    elif brand.casefold() in CARRIERS: add("carrier_as_brand","brand","Australian carrier must not be stored as manufacturer brand")
    if not model: add("missing_model_name","model_name","Model name is required")
    if not number: add("missing_model_number","model_number","Verified model identifier is required")
    if cat in {"mobile_phone","tablet","laptop","desktop","console"}:
        if not isinstance(storage,list) or not storage: add("missing_storage","storage_options","At least one structured storage option is required")
        elif any(not STORAGE.match(text(v)) for v in storage): add("invalid_storage","storage_options","Storage options must use numeric GB/TB values")
    network=text(row.get("network_generation") or row.get("network"))
    if cat in {"mobile_phone","tablet"} and re.search(r"(?:^|\D)3G(?:\D|$)",network,re.I) and not re.search(r"(?:4G|5G|LTE|VoLTE)",network,re.I): add("3g_only","network_generation","3G-only records are incompatible with the Australian post-3G-shutdown catalogue policy")
    return out

def key(row):
    storage=tuple(sorted(text(x).casefold() for x in row.get("storage_options",[]) if text(x)))
    return tuple(text(row.get(k)).casefold() for k in ("category","brand","model_name","model_number"))+(storage,)
def validate(rows):
    seen={}; results=[]
    for i,row in enumerate(rows):
        found=issues(row); k=key(row)
        if k in seen: found.append({"code":"duplicate_variant","field":"model_number","message":f"Duplicates row {seen[k]} for the same canonical model/storage variant"})
        else: seen[k]=i
        results.append({"index":i,"id":row.get("id"),"valid":not found,"issues":found})
    return results

def main(argv=None):
    p=argparse.ArgumentParser(); p.add_argument("path",type=Path); p.add_argument("--report",type=Path); a=p.parse_args(argv)
    payload=json.loads(a.path.read_text(encoding="utf-8")); rows=payload.get("rows",[payload]) if isinstance(payload,dict) else payload
    if not isinstance(rows,list) or any(not isinstance(r,dict) for r in rows): raise ValueError("Input must contain catalogue row objects")
    results=validate(rows); report={"rows":len(rows),"invalid":sum(not r["valid"] for r in results),"results":results}; encoded=json.dumps(report,indent=2)
    if a.report: a.report.write_text(encoded+"\n",encoding="utf-8")
    print(encoded); return 1 if report["invalid"] else 0
if __name__=="__main__": sys.exit(main())
