#!/usr/bin/env python3
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin

LOGIN_URL = "http://localhost:8080/login"
USERNAME = "testuser"
PASSWORD = "TestPass123!"
OUT_PATH = "Doc/Testing/login_post_response.txt"

s = requests.Session()
print(f"GET {LOGIN_URL}")
r = s.get(LOGIN_URL)
open(OUT_PATH, 'w', encoding='utf-8').write(f"GET {LOGIN_URL}\nStatus: {r.status_code}\n\n{r.text}\n\n")

soup = BeautifulSoup(r.text, 'html.parser')
form = soup.find('form')
if not form:
    print('No form found on /login')
    raise SystemExit(1)

action = form.get('action') or LOGIN_URL
post_url = urljoin(LOGIN_URL, action)

# collect inputs
data = {}
for inp in form.find_all('input'):
    name = inp.get('name')
    if not name:
        continue
    val = inp.get('value', '')
    data[name] = val

# heuristics for username/password fields
usr_keys = ['username', 'user', 'login', 'j_username']
pwd_keys = ['password', 'pass', 'j_password']
set_usr = False
set_pwd = False
for k in usr_keys:
    if k in data:
        data[k] = USERNAME
        set_usr = True
        break
for k in pwd_keys:
    if k in data:
        data[k] = PASSWORD
        set_pwd = True
        break
# fallback: try to set common names
if not set_usr:
    data['username'] = USERNAME
if not set_pwd:
    data['password'] = PASSWORD

print('POST', post_url)
resp = s.post(post_url, data=data, allow_redirects=False)
info = []
info.append(f'POST {post_url}')
info.append(f'Status: {resp.status_code}')
for k, v in resp.headers.items():
    info.append(f'{k}: {v}')

open(OUT_PATH, 'a', encoding='utf-8').write('\n'.join(info))
print('\n'.join(info))
print(f'Wrote evidence to {OUT_PATH}')
