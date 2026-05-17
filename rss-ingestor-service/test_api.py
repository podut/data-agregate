import requests
import json

BASE_URL = "http://localhost:8085"

def test_api():
    print("Testing /categories...")
    try:
        resp = requests.get(f"{BASE_URL}/categories")
        print(f"GET /categories: {resp.status_code}")
        print(json.dumps(resp.json(), indent=2))
    except Exception as e:
        print(f"Error: {e}")

    print("\nTesting /categories POST (add source)...")
    payload = {
        "name": "TestCategory",
        "rss_urls": ["https://news.ycombinator.com/rss"]
    }
    try:
        resp = requests.post(f"{BASE_URL}/categories", json=payload)
        print(f"POST /categories: {resp.status_code}")
        print(resp.json())
    except Exception as e:
        print(f"Error: {e}")

    print("\nTesting /categories again...")
    try:
        resp = requests.get(f"{BASE_URL}/categories")
        print(json.dumps(resp.json(), indent=2))
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_api()
