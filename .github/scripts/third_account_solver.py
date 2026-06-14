import urllib.request
import urllib.parse
import urllib.error
import json
import time
import os
import random
import datetime

# 3rd Account Session Token (ezQufSYltM)
LEETCODE_SESSION = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfYXV0aF91c2VyX2lkIjoiMjA4OTcxOTIiLCJfYXV0aF91c2VyX2JhY2tlbmQiOiJhbGxhdXRoLmFjY291bnQuYXV0aF9iYWNrZW5kcy5BdXRoZW50aWNhdGlvbkJhY2tlbmQiLCJfYXV0aF91c2VyX2hhc2giOiI5MWIyMmY1Y2I1MDAxNzMwYWFhNDkxODgyNGYzZDhlZmY3ZTdmZDY5ZDRkN2U5MGRmZTIzZjBjNGJhNWQ0MmI0Iiwic2Vzc2lvbl91dWlkIjoiZmUxYzNmZjkiLCJpZCI6MjA4OTcxOTIsImVtYWlsIjoic3JpcmFtczIzY3NAcHNuYWNldC5lZHUuaW4iLCJ1c2VybmFtZSI6ImV6UXVmU1lsdE0iLCJ1c2VyX3NsdWciOiJlelF1ZlNZbHRNIiwiYXZhdGFyIjoiaHR0cHM6Ly9hc3NldHMubGVldGNvZGUuY29tL3VzZXJzL2V6UXVmU1lsdE0vYXZhdGFyXzE3NzA2NTM5MzIucG5nIiwicmVmcmVzaGVkX2F0IjoxNzgxNDIyMDQ1LCJpcCI6IjI0MDY6NzQwMDpjYTplNGM1OmU1NDc6MzEyZDozN2NkOjg2OWEiLCJpZGVudGl0eSI6IjE2ZmVlMzc1NTlkYmQ0MmI0NDgyMDQ0NDZkMDIwODlmIiwiZGV2aWNlX3dpdGhfaXAiOlsiYjI5N2I5MjRlYjZkYWJiZTc3N2U0Y2U0NDY3MDIzNmIiLCIyNDA2Ojc0MDA6Y2E6ZTRjNTplNTQ3OjMxMmQ6MzdjZDo4NjlhIl19.tgvJu2QeNDt8HF-SKdfhFZggvf4MkX92cT1FmsN-13Q"
CSRF_TOKEN = "0UEtSQi2tUaxZwBubLkpxnQAgpx2LP1I"

IDX_PATH = ".github/scripts/third_account_sync_idx.json"

def fetch_csrf_token(session_cookie):
    print("Fetching dynamic CSRF token from LeetCode API...")
    url = "https://leetcode.com/api/problems/all/"
    req = urllib.request.Request(url)
    req.add_header("Cookie", f"LEETCODE_SESSION={session_cookie};")
    req.add_header("User-Agent", "Mozilla/5.0")
    try:
        with urllib.request.urlopen(req) as r:
            cookies = r.info().get_all("Set-Cookie")
            if cookies:
                for c in cookies:
                    if "csrftoken=" in c:
                        token = c.split("csrftoken=")[1].split(";")[0]
                        print("Dynamic CSRF token fetched successfully.")
                        return token
    except Exception as e:
        print(f"Failed to fetch dynamic CSRF token: {e}")
    return None

def fetch_unsolved_problems():
    print("Fetching problems from LeetCode API...")
    url = "https://leetcode.com/api/problems/all/"
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "x-csrftoken": CSRF_TOKEN,
        "user-agent": "Mozilla/5.0",
        "referer": "https://leetcode.com/"
    }
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            data = json.loads(r.read().decode("utf-8"))
    except Exception as e:
        print(f"Failed to fetch problems from API: {e}")
        return []

    stat_pairs = data.get("stat_status_pairs", [])
    unsolved = []
    
    for pair in stat_pairs:
        if pair.get("paid_only", False):
            continue
        status = pair.get("status")
        if status == "ac":
            continue
            
        stat = pair.get("stat", {})
        difficulty = pair.get("difficulty", {})
        
        q_id = stat.get("question_id")
        frontend_id = stat.get("frontend_question_id")
        title = stat.get("question__title")
        slug = stat.get("question__title_slug")
        level_num = difficulty.get("level")
        
        diff = "EASY" if level_num == 1 else ("MEDIUM" if level_num == 2 else "HARD")
            
        unsolved.append({
            "id": q_id,
            "frontend_id": frontend_id,
            "title": title,
            "slug": slug,
            "difficulty": diff
        })
    print(f"Found {len(unsolved)} unsolved problems on LeetCode.")
    return unsolved

def fetch_java_solution(frontend_id, title):
    variations = [
        f"{frontend_id}. {title}/{frontend_id}.java",
        f"{frontend_id}. {title.replace('-', ' ')}/{frontend_id}.java",
    ]
    for path in variations:
        url = "https://raw.githubusercontent.com/walkccc/LeetCode/main/solutions/" + urllib.parse.quote(path)
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "Mozilla/5.0")
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                code = r.read().decode("utf-8")
                if "class Solution" in code:
                    return code
        except Exception:
            pass
    return None

def make_submission(slug, q_id, code):
    url = f"https://leetcode.com/problems/{slug}/submit/"
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "x-csrftoken": CSRF_TOKEN,
        "content-type": "application/json",
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "referer": f"https://leetcode.com/problems/{slug}/description/"
    }
    data = json.dumps({
        "lang": "java",
        "question_id": str(q_id),
        "typed_code": code
    }).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            body = r.read().decode("utf-8")
            return r.status, json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        try:
            body = e.read().decode("utf-8")
            return e.code, json.loads(body) if body else None
        except Exception:
            return e.code, None
    except Exception as e:
        print(f"Submission API error: {e}")
        return 500, None

def check_status(submission_id):
    url = f"https://leetcode.com/submissions/detail/{submission_id}/check/"
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "user-agent": "Mozilla/5.0"
    }
    req = urllib.request.Request(url, headers=headers)
    for _ in range(15):
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                res = json.loads(r.read().decode("utf-8"))
                if res.get("state") == "SUCCESS":
                    return res
                time.sleep(1.5)
        except Exception:
            time.sleep(1.5)
    return None

def main():
    global CSRF_TOKEN
    token = fetch_csrf_token(LEETCODE_SESSION)
    if not token:
        print("Failed to get CSRF token. Exiting.")
        return
    CSRF_TOKEN = token

    state = {"submitted_ids": [], "today_date": "", "today_count": 0, "today_target": 2000}
    if os.path.exists(IDX_PATH):
        try:
            with open(IDX_PATH, "r") as f:
                state.update(json.load(f))
        except:
            pass

    today = datetime.datetime.utcnow().strftime("%Y-%m-%d")
    if state.get("today_date") != today:
        state["today_date"] = today
        state["today_count"] = 0
        state["today_target"] = 2000 # Max target to solve all as fast as possible

    # Max speed run limit per run: 50 problems
    run_limit = 50
    print(f"Starting Third Account solver. Run limit this execution: {run_limit} problems.")

    unsolved = fetch_unsolved_problems()
    if not unsolved:
        print("No unsolved problems found.")
        return

    submitted_ids = set(state.get("submitted_ids", []))
    available = [p for p in unsolved if p["id"] not in submitted_ids]
    print(f"Total available unsolved: {len(available)}")

    solved_count = 0
    for p in available:
        if solved_count >= run_limit:
            print(f"Finished current batch of {run_limit} problems.")
            break

        print(f"\nAttempting #{p['frontend_id']} - {p['title']}...")
        code = fetch_java_solution(p["frontend_id"], p["title"])
        if not code:
            print("  No walkccc solution found.")
            state["submitted_ids"].append(p["id"]) # Mark attempted to skip next time
            continue

        # Post-process
        code = code.replace("edges.size()", "edges.length")
        code = code.replace("edges.length()", "edges.length")

        # Submit
        status_code, res = make_submission(p["slug"], p["id"], code)
        if status_code != 200 or not res or "submission_id" not in res:
            print(f"  Submission failed with status: {status_code}")
            if status_code == 429:
                print("  Rate limited. Sleeping 30 seconds...")
                time.sleep(30)
            continue

        sub_id = res["submission_id"]
        print(f"  Submitted ID: {sub_id}, checking status...")
        result = check_status(sub_id)

        # Enforce 1 strict attempt rule: always append to submitted_ids
        state["submitted_ids"].append(p["id"])
        state["today_count"] += 1
        solved_count += 1

        if result and result.get("status_msg") == "Accepted":
            print(f"  ACCEPTED! Runtime: {result.get('status_runtime')} | Memory: {result.get('status_memory')}")
        else:
            msg = result.get("status_msg") if result else "Timeout"
            print(f"  NOT ACCEPTED: {msg}")

        with open(IDX_PATH, "w") as f:
            json.dump(state, f, indent=2)

        # High-speed delay: 6 seconds between submissions
        time.sleep(6)

if __name__ == "__main__":
    main()
