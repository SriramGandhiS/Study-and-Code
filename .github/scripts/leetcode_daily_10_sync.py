import urllib.request
import urllib.parse
import urllib.error
import json
import time
import os
import random
import datetime

# =========================================================================
# CONFIGURATION & FALLBACKS
# =========================================================================
# Default fallback credentials from user session
FALLBACK_SESSION = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfYXV0aF91c2VyX2lkIjoiMTEzNTkyMTgiLCJfYXV0aF91c2VyX2JhY2tlbmQiOiJhbGxhdXRoLmFjY291bnQuYXV0aF9iYWNrZW5kcy5BdXRoZW50aWNhdGlvbkJhY2tlbmQiLCJfYXV0aF91c2VyX2hhc2giOiJhZWFkY2E4OWJhMTQxNzcwZTRmODJlMDBmODhjMjVhOGMzYjJkODMzYzA4ZDQyZTQ0NTExNjZhNDU1NmU1MzU1Iiwic2Vzc2lvbl91dWlkIjoiY2UyMDU0MGEiLCJpZCI6MTEzNTkyMTgsImVtYWlsIjoiaWFtcmFtbThAZ21haWwuY29tIiwidXNlcm5hbWUiOiJpYW1yYW1tOCIsInVzZXJfc2x1ZyI6ImlhbXJhbW04IiwiYXZhdGFyIjoiaHR0cHM6Ly9hc3NldHMubGVldGNvZGUuY29tL3VzZXJzL2lhbXJhbW04L2F2YXRhcl8xNzgxMTg2NTQzLnBuZyIsInJlZnJlc2hlZF9hdCI6MTc4MTQxMjk3NiwiaXAiOiIyNDA2Ojc0MDA6Y2E6ZTRjNTplNTQ3OjMxMmQ6MzdjZDo4NjlhIiwiaWRlbnRpdHkiOiIxNmZlZTM3NTU5ZGJkNDJiNDQ4MjA0NDQ2ZDAyMDg5ZiIsImRldmljZV93aXRoX2lwIjpbIjk2ZGJjNjE3OTEzMmZhYzUzZDcyOWRkZWEwN2VkZjYzIiwiMjQwNjo3NDAwOmNhOmU0YzU6ZTU0NzozMTJkOjM3Y2Q6ODY5YSJdfQ.QS3qCz_8fpoGYYc5kdWDikr-X5A1e6GRRHoUY3lc8eY"
FALLBACK_CSRF    = "8qiq9f3LU6cXm98UZyNohtvTePpcHWLG"

LEETCODE_SESSION = FALLBACK_SESSION
CSRF_TOKEN      = os.environ.get("CSRF_TOKEN", FALLBACK_CSRF)

IDX_PATH = ".github/scripts/leetcode_sync_idx.json"

# =========================================================================
# 1. DYNAMIC CSRF TOKEN FETCHING
# =========================================================================
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

# =========================================================================
# 2. LIVE LEETCODE API - GET UNSOLVED PROBLEMS WITH DETAILS
# =========================================================================
def fetch_unsolved_problems():
    print("Fetching problems from LeetCode API...")
    url = "https://leetcode.com/api/problems/all/"
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "x-csrftoken": CSRF_TOKEN,
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
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
        # Skip paid-only problems
        if pair.get("paid_only", False):
            continue
            
        # status can be 'ac' (solved) or 'notac' or None
        status = pair.get("status")
        if status == "ac":
            continue
            
        stat = pair.get("stat", {})
        difficulty = pair.get("difficulty", {})
        
        q_id = stat.get("question_id")
        frontend_id = stat.get("frontend_question_id")
        title = stat.get("question__title")
        slug = stat.get("question__title_slug")
        level_num = difficulty.get("level")  # 1 = Easy, 2 = Medium, 3 = Hard
        
        if level_num == 1:
            diff = "EASY"
        elif level_num == 2:
            diff = "MEDIUM"
        else:
            diff = "HARD"
            
        unsolved.append({
            "id": q_id,
            "frontend_id": frontend_id,
            "title": title,
            "slug": slug,
            "difficulty": diff
        })
        
    print(f"Found {len(unsolved)} unsolved problems on LeetCode.")
    return unsolved

def fetch_problem_details(slug):
    print(f"Fetching problem details for {slug} via GraphQL...")
    url = "https://leetcode.com/graphql/"
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "x-csrftoken": CSRF_TOKEN,
        "content-type": "application/json",
        "referer": f"https://leetcode.com/problems/{slug}/description/",
        "user-agent": "Mozilla/5.0"
    }
    query = {
        "query": """
        query questionData($titleSlug: String!) {
          question(titleSlug: $titleSlug) {
            questionId
            questionFrontendId
            title
            titleSlug
            content
            difficulty
            codeSnippets {
              lang
              langSlug
              code
            }
          }
        }
        """,
        "variables": {
            "titleSlug": slug
        }
    }
    req = urllib.request.Request(url, data=json.dumps(query).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            res = json.loads(r.read().decode("utf-8"))
            q = res.get("data", {}).get("question", {})
            return q
    except Exception as e:
        print(f"Failed to fetch problem details: {e}")
        return None

# =========================================================================
# 3. LEETCODE DAILY CODING CHALLENGE
# =========================================================================
def fetch_daily_challenge():
    print("Fetching LeetCode Daily Coding Challenge...")
    url = "https://leetcode.com/graphql/"
    query = {
        "query": """
        query questionOfToday {
          activeDailyCodingChallengeQuestion {
            date
            userStatus
            link
            question {
              questionId
              questionFrontendId
              title
              titleSlug
              difficulty
              content
            }
          }
        }
        """
    }
    headers = {
        "cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "x-csrftoken": CSRF_TOKEN,
        "content-type": "application/json",
        "user-agent": "Mozilla/5.0"
    }
    req = urllib.request.Request(url, data=json.dumps(query).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            res = json.loads(r.read().decode("utf-8"))
            return res.get("data", {}).get("activeDailyCodingChallengeQuestion", {})
    except Exception as e:
        print(f"Failed to fetch Daily Challenge: {e}")
        return None

# =========================================================================
# 4. DYNAMIC SOLUTIONS SCRAPER (WALKCCC & DOOCS)
# =========================================================================
def fetch_solution_from_walkccc(frontend_id, title):
    # walkccc format: solutions/{id}. {title}/{id}.java
    variations = [
        f"{frontend_id}. {title}/{frontend_id}.java",
        f"{frontend_id}. {title.replace('-', ' ')}/{frontend_id}.java",
    ]
    for path in variations:
        url = "https://raw.githubusercontent.com/walkccc/LeetCode/main/solutions/" + urllib.parse.quote(path)
        print(f"Trying walkccc URL: {url}")
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "Mozilla/5.0")
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                code = r.read().decode("utf-8")
                if "class " in code:
                    print(f"Successfully fetched solution from walkccc!")
                    return code
        except Exception as e:
            pass
    return None

def fetch_solution_from_doocs(frontend_id_str, title):
    # doocs format: solution/{range}/{padded_id}.{title}/Solution.java
    try:
        frontend_id = int(frontend_id_str)
    except:
        return None
    lower_limit = (frontend_id // 100) * 100
    upper_limit = lower_limit + 99
    range_str = f"{lower_limit:04d}-{upper_limit:04d}"
    
    variations = [
        f"solution/{range_str}/{frontend_id:04d}.{title.replace('-', ' ').title().replace(' ', '')}/Solution.java",
        f"solution/{range_str}/{frontend_id:04d}.{title}/Solution.java",
        f"solution/{range_str}/{frontend_id:04d}.{title.replace('-', ' ')}/Solution.java",
        f"solution/{range_str}/{frontend_id:04d}.{title.replace(' ', '-')}/Solution.java",
    ]
    for path in variations:
        url = "https://raw.githubusercontent.com/doocs/leetcode/main/" + urllib.parse.quote(path)
        print(f"Trying doocs URL: {url}")
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "Mozilla/5.0")
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                code = r.read().decode("utf-8")
                if "class " in code:
                    print(f"Successfully fetched solution from doocs!")
                    return code
        except Exception as e:
            pass
    return None

def fetch_java_solution(frontend_id, title):
    code = fetch_solution_from_walkccc(frontend_id, title)
    if code:
        return code
    code = fetch_solution_from_doocs(frontend_id, title)
    if code:
        return code
    return None

# =========================================================================
# 5. GEMINI SOLVER FALLBACK
# =========================================================================
def solve_with_gemini(frontend_id, title, difficulty, content):
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY environment variable is missing. Skipping Gemini generation.")
        return None
        
    print(f"Calling Gemini API to generate solution for #{frontend_id} - {title}...")
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
    
    prompt = (
        f"Write a complete, optimized Java solution class (named 'Solution') for the following LeetCode problem.\n"
        f"Problem ID: {frontend_id}\n"
        f"Problem Title: {title}\n"
        f"Difficulty: {difficulty}\n"
        f"Problem Description:\n{content}\n\n"
        f"Rules:\n"
        f"1. Return ONLY the pure Java source code. Do not wrap it in anything else other than a markdown ```java block.\n"
        f"2. Ensure all helper classes or imports are included. The class name must be 'Solution'.\n"
        f"3. Make sure the solution is correct, efficient, and passes standard LeetCode test cases.\n"
        f"4. Critical Java Syntax: For arrays (like `int[]` or `int[][]` or similar), use `.length` to get the size (e.g. `edges.length`). DO NOT use `.size()` or `.length()` on raw arrays!\n"
        f"5. Do NOT include any explanations, documentation, comments, or author info inside the code. Output only clean, raw Java code so it looks like a normal developer's script commit."
    )
    
    data = {
        "contents": [{
            "parts": [{
                "text": prompt
            }]
        }]
    }
    
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            res = json.loads(r.read().decode("utf-8"))
            candidates = res.get("candidates", [])
            if candidates:
                text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                if "```java" in text:
                    code = text.split("```java")[1].split("```")[0].strip()
                elif "```" in text:
                    code = text.split("```")[1].split("```")[0].strip()
                else:
                    code = text.strip()
                
                # Auto-correction post-processing for common LLM array syntax errors in Java
                code = code.replace("edges.size()", "edges.length")
                code = code.replace("edges.length()", "edges.length")
                return code
    except Exception as e:
        print(f"Gemini API call failed: {e}")
    return None

# =========================================================================
# 6. LEETCODE SUBMISSION
# =========================================================================
def make_submission(slug, question_id, code):
    url = f"https://leetcode.com/problems/{slug}/submit/"
    headers = {
        "x-csrftoken":  CSRF_TOKEN,
        "cookie":       f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};",
        "referer":      f"https://leetcode.com/problems/{slug}/description/",
        "content-type": "application/json",
        "user-agent":   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    }
    data = {"lang": "java", "question_id": str(question_id), "typed_code": code}
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            body = r.read().decode("utf-8")
            return r.status, json.loads(body) if body else None
    except Exception as e:
        print(f"Error submitting {slug}: {e}")
        return 500, None

def check_status(submission_id):
    url = f"https://leetcode.com/submissions/detail/{submission_id}/check/"
    headers = {"cookie": f"LEETCODE_SESSION={LEETCODE_SESSION}; csrftoken={CSRF_TOKEN};", "user-agent": "Mozilla/5.0"}
    req = urllib.request.Request(url, headers=headers)
    for _ in range(12):
        try:
            with urllib.request.urlopen(req) as r:
                res = json.loads(r.read().decode("utf-8"))
                if res.get("state") == "SUCCESS":
                    return res
                time.sleep(4)
        except Exception:
            time.sleep(4)
    return None

def perform_checkin(session_cookie, csrf_token):
    print("Attempting LeetCode daily check-in...")
    url = "https://leetcode.com/graphql/"
    query = {
        "query": """
        mutation checkin {
          checkin {
            checkedIn
            ok
          }
        }
        """
    }
    headers = {
        "cookie": f"LEETCODE_SESSION={session_cookie}; csrftoken={csrf_token};",
        "x-csrftoken": csrf_token,
        "content-type": "application/json",
        "user-agent": "Mozilla/5.0",
        "referer": "https://leetcode.com/"
    }
    req = urllib.request.Request(url, data=json.dumps(query).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as r:
            res = json.loads(r.read().decode("utf-8"))
            checkin_res = res.get("data", {}).get("checkin", {})
            if checkin_res.get("ok"):
                print(f"Daily check-in successful! Checked In status: {checkin_res.get('checkedIn')}")
            else:
                print("Daily check-in returned not OK (already checked in today).")
    except Exception as e:
        print(f"Daily check-in failed: {e}")

# =========================================================================
# 7. SCHEDULER & MAIN FLOW
# =========================================================================
def main():
    global CSRF_TOKEN
    print(f"=== LeetCode Fully Autonomous Solver started at {datetime.datetime.utcnow().isoformat()} ===")
    
    # 1. Dynamically retrieve the CSRF token to prevent 403 Forbidden errors
    dynamic_csrf = fetch_csrf_token(LEETCODE_SESSION)
    if dynamic_csrf:
        CSRF_TOKEN = dynamic_csrf
        print(f"Using dynamic CSRF token: {CSRF_TOKEN[:6]}...")
    else:
        print("Using environment fallback CSRF token.")
        
    # Perform daily check-in to claim daily points
    perform_checkin(LEETCODE_SESSION, CSRF_TOKEN)
        
    # Load state
    state = {"submitted_ids": [], "today_date": "", "today_count": 0, "today_target": 0}
    if os.path.exists(IDX_PATH):
        try:
            with open(IDX_PATH, "r") as f:
                loaded = json.load(f)
                state.update(loaded)
        except Exception:
            pass
            
    test_mode = os.environ.get("TEST_MODE") == "true"
    test_limit = int(os.environ.get("TEST_LIMIT", "3"))
    
    if test_mode:
        print(f"TEST_MODE is enabled! Will attempt to solve {test_limit} problems immediately.")
        run_count = test_limit
    else:
        today = datetime.datetime.utcnow().strftime("%Y-%m-%d")
        
        # Choose new target if it's a new day
        if state.get("today_date") != today:
            target = random.randint(20, 30)
            state["today_date"] = today
            state["today_count"] = 0
            state["today_target"] = target
            print(f"New day initialized. Daily target: {target} problems.")
            if os.path.dirname(IDX_PATH):
                os.makedirs(os.path.dirname(IDX_PATH), exist_ok=True)
            with open(IDX_PATH, "w") as f:
                json.dump(state, f, indent=2)
                
        today_count = state.get("today_count", 0)
        today_target = state.get("today_target", 24)
        
        print(f"Daily progress: {today_count}/{today_target} solved today.")
        
        if today_count >= today_target:
            print(f"Already completed daily target of {today_target} problems. Skipping.")
            return
            
        remaining_problems = today_target - today_count
        current_hour = datetime.datetime.utcnow().hour
        remaining_hours = 24 - current_hour
        
        print(f"Remaining problems: {remaining_problems}, Remaining hours today: {remaining_hours}")
        
        # Per run, aim to submit 2-3 problems to hit 20-30 daily
        hours_remaining = max(1, 24 - datetime.datetime.utcnow().hour)
        needed_per_run = max(1, -(-remaining_problems // hours_remaining))  # ceil div
        run_count = min(3, max(1, needed_per_run))
        
        roll = random.random()
        prob = min(1.0, remaining_problems / max(1, hours_remaining))
        should_run = roll < prob or remaining_problems >= hours_remaining
        print(f"Prob={prob:.2f} roll={roll:.2f} -> {'RUN' if should_run else 'SKIP'} | run_count={run_count}")
            
        if not should_run:
            print("Decided to skip this hour. Spacing out runs.")
            return
            
        # Light randomized delay (1 to 10 minutes)
        delay = random.randint(60, 600)
        print(f"Organic delay: Sleeping {delay} seconds...")
        time.sleep(delay)
        
    solved_in_this_run = 0

    # -------------------------------------------------------------------------
    # STEP A: SOLVE LEETCODE DAILY CODING CHALLENGE (STREAK AND BADGES)
    # -------------------------------------------------------------------------
    if not test_mode:
        daily_challenge = fetch_daily_challenge()
        if daily_challenge:
            date_challenge = daily_challenge.get("date")
            user_status = daily_challenge.get("userStatus")
            q_info = daily_challenge.get("question", {})
            slug = q_info.get("titleSlug")
            frontend_id = q_info.get("questionFrontendId")
            q_id = q_info.get("questionId")
            title = q_info.get("title")
            difficulty = q_info.get("difficulty")
            content = q_info.get("content")

            print(f"\n--- LeetCode Daily Coding Challenge ({date_challenge}) ---")
            print(f"Problem: #{frontend_id} - {title} ({difficulty})")
            print(f"Status: {user_status}")

            if user_status == "Finish":
                print("Daily Coding Challenge is already solved! Streak is safe. ✅")
            elif not slug:
                print("Could not retrieve daily challenge slug.")
            else:
                print("Daily Coding Challenge is NOT solved. Solving it now to protect streak! ⚡")
                # 1. Try public solutions walkccc/doocs
                code = fetch_java_solution(frontend_id, title)
                if not code:
                    # 2. Fallback to Gemini LLM generation
                    code = solve_with_gemini(frontend_id, title, difficulty, content)

                if code:
                    # Auto-correction post-processing for common array syntax errors in Java (fixes walkccc/doocs/Gemini issues)
                    code = code.replace("edges.size()", "edges.length")
                    code = code.replace("edges.length()", "edges.length")
                    
                    print("Submitting Daily Challenge solution to LeetCode...")
                    status, res = make_submission(slug, q_id, code)
                    if status == 200 and res and "submission_id" in res:
                        sub_id = res["submission_id"]
                        print(f"Daily Challenge Submission ID: {sub_id}, checking result...")
                        result = check_status(sub_id)
                        if result and result.get("status_msg") == "Accepted":
                            print(f"DAILY CHALLENGE ACCEPTED! Streak incremented! 🎉")
                            # Save locally
                            local_dir = f"dsa/dsa {frontend_id} - {slug}"
                            os.makedirs(local_dir, exist_ok=True)
                            with open(f"{local_dir}/Solution.java", "w", encoding="utf-8") as f:
                                f.write(code)
                            
                            # Track state
                            state["submitted_ids"].append(q_id)
                            state["today_count"] += 1
                            solved_in_this_run += 1
                            with open(IDX_PATH, "w") as f:
                                json.dump(state, f, indent=2)
                        else:
                            msg = result.get("status_msg") if result else "Timeout"
                            print(f"Daily Challenge submission NOT accepted: {msg}")
                    else:
                        print(f"Failed to submit Daily Challenge: {status}")
                else:
                    print("Failed to find or generate solution for LeetCode Daily Challenge.")

    # -------------------------------------------------------------------------
    # STEP B: GENERAL SOLVER FOR ADDITIONAL PROBLEMS (DAILY TARGET ACHIEVEMENT)
    # -------------------------------------------------------------------------
    # Fetch all unsolved problems from LeetCode
    unsolved = fetch_unsolved_problems()
    if not unsolved:
        print("No unsolved problems found.")
        return
        
    for iteration in range(run_count):
        # If we solved the daily challenge in this run, count it
        if solved_in_this_run >= run_count:
            print("Target problem count for this run reached.")
            break

        if iteration > 0 or solved_in_this_run > 0:
            print("Sleeping 20 seconds between submissions...")
            time.sleep(20)
            
        # Refresh submitted_ids from state
        submitted_ids = set(state.get("submitted_ids", []))
        available = [p for p in unsolved if p["id"] not in submitted_ids]
        
        if not available:
            print("No more available unsolved problems.")
            break
            
        # Group by difficulty
        by_diff = {"EASY": [], "MEDIUM": [], "HARD": []}
        for p in available:
            by_diff[p["difficulty"]].append(p)
            
        print(f"Pool sizes - Easy: {len(by_diff['EASY'])}, Medium: {len(by_diff['MEDIUM'])}, Hard: {len(by_diff['HARD'])}")
        
        success = False
        diff_preference = ["HARD", "MEDIUM", "EASY"]
        roll = random.random()
        if roll < 0.60:
            start_diff = "HARD"
        elif roll < 0.90:
            start_diff = "MEDIUM"
        else:
            start_diff = "EASY"
            
        diff_preference.remove(start_diff)
        diff_preference.insert(0, start_diff)
        
        picked_problem = None
        java_code = None
        
        for d in diff_preference:
            pool = by_diff[d]
            if not pool:
                continue
            random.shuffle(pool)
            for p in pool:
                print(f"Attempting to fetch solution for #{p['frontend_id']} - {p['title']} ({d})")
                code = fetch_java_solution(p["frontend_id"], p["title"])
                if code:
                    # Auto-correction post-processing for common array syntax errors in Java
                    code = code.replace("edges.size()", "edges.length")
                    code = code.replace("edges.length()", "edges.length")
                    
                    picked_problem = p
                    java_code = code
                    success = True
                    break
            if success:
                break
                
        if not success or not picked_problem or not java_code:
            print("Could not find any public solution for any unsolved problem in the pool.")
            break
            
        print(f"\n--- [{iteration+1}/{run_count}] Selected: #{picked_problem['frontend_id']} - {picked_problem['title']} ({picked_problem['difficulty']}) ---")
        
        # Submit to LeetCode
        print("Submitting solution to LeetCode...")
        status, res = make_submission(picked_problem["slug"], picked_problem["id"], java_code)
        if status != 200 or not res or "submission_id" not in res:
            print(f"LeetCode submission failed: {status}. Skipping.")
            continue
            
        sub_id = res["submission_id"]
        print(f"Submitted ID: {sub_id}, checking result...")
        result = check_status(sub_id)
        
        if not result or result.get("status_msg") != "Accepted":
            msg = result.get("status_msg") if result else "Timeout"
            print(f"NOT accepted: {msg} — Skipping this problem.")
            continue
            
        print(f"ACCEPTED! Runtime: {result.get('status_runtime')}, Memory: {result.get('status_memory')}")
        
        # Write solution file locally
        local_dir = f"dsa/dsa {picked_problem['frontend_id']} - {picked_problem['slug']}"
        os.makedirs(local_dir, exist_ok=True)
        with open(f"{local_dir}/Solution.java", "w", encoding="utf-8") as f:
            f.write(java_code)
        print(f"Wrote solution locally to: {local_dir}/Solution.java")
        
        # Update local state
        state["submitted_ids"].append(picked_problem["id"])
        if not test_mode:
            state["today_count"] += 1
        solved_in_this_run += 1
        
        if os.path.dirname(IDX_PATH):
            os.makedirs(os.path.dirname(IDX_PATH), exist_ok=True)
        with open(IDX_PATH, "w") as f:
            json.dump(state, f, indent=2)
            
        print(f"Problem #{picked_problem['frontend_id']} solved successfully and tracked.")
        
    print(f"=== Completed. Solved {solved_in_this_run}/{run_count} problems this run. ===")

if __name__ == "__main__":
    main()
