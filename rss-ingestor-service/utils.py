import re

def clean_html(raw_html: str) -> str:
    if not raw_html: return ""
    clean = re.sub(r'<[^>]+>', ' ', raw_html)
    entities = {
        '&nbsp;': ' ', '&amp;': '&', '&lt;': '<', '&gt;': '>',
        '&quot;': '"', '&#8217;': "'", '&#8230;': '...'
    }
    for ent, val in entities.items():
        clean = clean.replace(ent, val)
    clean = re.sub(r'\s+', ' ', clean).strip()
    return clean

def google_news_rss_url(query: str, region: str) -> str:
    from urllib.parse import quote_plus
    region_upper = region.upper()
    lang = region.lower()
    return (
        f"https://news.google.com/rss/search"
        f"?q={quote_plus(query)}"
        f"&hl={lang}"
        f"&gl={region_upper}"
        f"&ceid={region_upper}:{lang}"
    )

def rank_articles(articles: list, favorite_keywords: list) -> list:
    """
    Ranks articles with a diversity mechanism to prevent single-category dominance.
    """
    # 1. Base scoring with interest boosts
    keywords = [k.lower().strip() for k in favorite_keywords if k.strip()]
    processed = []
    
    for a in articles:
        base_score = a.get("score", 70)
        title_low = (a.get("title") or "").lower()
        cat_low = (a.get("category") or "").lower()
        tags = [t.lower() for t in a.get("tags", [])]
        
        boost = 0
        for kw in keywords:
            if kw in cat_low: boost += 25
            if kw in title_low: boost += 15
            if any(kw in t for t in tags): boost += 10
        
        a["_tmp_score"] = base_score + boost
        processed.append(a)

    # Sort initially by raw interest score
    processed.sort(key=lambda x: x["_tmp_score"], reverse=True)

    # 2. Diversity filtering
    final = []
    cat_counts = {}
    for a in processed:
        cat = (a.get("category") or "General").lower()
        count = cat_counts.get(cat, 0)
        
        # Penalizare mai blanda: -5 per articol existent (inainte era -15)
        # Astfel, dupa 10 articole avem doar -50, deci scorul ramane vizibil
        penalty = count * 5
        a["score"] = max(1, min(100, a["_tmp_score"] - penalty))
        
        cat_counts[cat] = count + 1
        final.append(a)

    # Sortare finala: scor DESC, apoi data DESC
    return sorted(final, key=lambda x: (x["score"], x.get("publish_date", "")), reverse=True)

async def scrape_image(url: str) -> str | None:
    """Incearca sa gaseasca o imagine reprezentativa in pagina (og:image sau prima imagine mare)."""
    import httpx
    from bs4 import BeautifulSoup
    from urllib.parse import urljoin

    try:
        async with httpx.AsyncClient(timeout=5.0, follow_redirects=True) as client:
            r = await client.get(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
            if r.status_code != 200: return None
            
            soup = BeautifulSoup(r.text, 'html.parser')
            
            # 1. Cauta OpenGraph Image
            og_img = soup.find("meta", property="og:image") or soup.find("meta", attrs={"name": "og:image"})
            if og_img and og_img.get("content"):
                return urljoin(url, og_img["content"])
            
            # 2. Cauta Twitter Image
            tw_img = soup.find("meta", attrs={"name": "twitter:image"})
            if tw_img and tw_img.get("content"):
                return urljoin(url, tw_img["content"])
                
            # 3. Fallback: prima imagine cu dimensiuni rezonabile (ignorand iconite)
            for img in soup.find_all("img"):
                src = img.get("src")
                if not src: continue
                # Ignoram tracking pixels, iconite mici, spinner-e
                if any(x in src.lower() for x in ["icon", "logo", "avatar", "ads", "track", "pixel"]): continue
                return urljoin(url, src)
                
    except Exception as e:
        print(f"[Scrape] Eroare la extragere imagine pentru {url}: {e}")
    return None

