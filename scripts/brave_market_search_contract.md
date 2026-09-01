# Brave market search contract

`brave_market_search_contract.py` is intentionally narrow. It verifies that the Android laptop flow points at the Brave-first backend, the backend reads `BRAVE_SEARCH_API_KEY` only at runtime, Brave is first in provider priority, SerpApi is fallback-only, and Gumtree/Facebook public indexed discovery remains covered without treating marketplace asking prices as sold-comparable authority.
