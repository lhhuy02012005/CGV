#!/usr/bin/env python3
"""Local admin smoke test; creates one disposable account and soft-deletes it."""
import json
import os
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get('CGV_GATEWAY', 'http://localhost:8000/api/v1')
EMAIL = os.environ['CGV_TEST_ADMIN_EMAIL']
PASSWORD = os.environ['CGV_TEST_ADMIN_PASSWORD']


def request(path, method='GET', body=None, token=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    payload = json.dumps(body).encode() if body is not None else None
    call = urllib.request.Request(BASE + path, payload, headers, method=method)
    try:
        with urllib.request.urlopen(call, timeout=15) as response:
            assert response.status == expected, (path, response.status, expected)
            content = response.read()
            return json.loads(content) if content else None
    except urllib.error.HTTPError as error:
        if error.code == expected:
            return None
        raise AssertionError((path, error.code, error.read().decode()[:300])) from error


def main():
    token = request('/auth/login', 'POST', {'username': EMAIL, 'password': PASSWORD})['data']['accessToken']
    marker = uuid.uuid4().hex[:10]
    email = f'qa-admin-{marker}@example.invalid'
    password = 'Test-' + uuid.uuid4().hex[:14]
    created = None
    try:
        created = request('/users', 'POST', {
            'fullName': 'Admin Test', 'email': email, 'phone': '', 'username': email,
            'password': password, 'role': 'TICKET_STAFF'
        }, token, 200)['data']
        user_id = created['id']
        assert created['roles'] == ['TICKET_STAFF']
        response = request('/users?page=1&size=100', token=token)['data']
        assert any(u['id'] == user_id for u in response['data'])
        updated = request(f'/users/{user_id}', 'PATCH', {
            'fullName': 'Admin Test Edited', 'email': email, 'phone': '0900000000'
        }, token)['data']
        assert updated['fullName'] == 'Admin Test Edited' and updated['phone'] == '0900000000'
        request(f'/users/{user_id}/roles', 'POST', {'role': 'CONTENT_MANAGER'}, token)
        listed = request('/users?page=1&size=100', token=token)['data']['data']
        assert next(u for u in listed if u['id'] == user_id)['roles'] == ['CONTENT_MANAGER']
        request(f'/users/{user_id}/roles/CONTENT_MANAGER', 'DELETE', token=token)
        request(f'/users/{user_id}/roles', 'POST', {'role': 'USER'}, token)
        listed = request('/users?page=1&size=100', token=token)['data']['data']
        assert next(u for u in listed if u['id'] == user_id)['roles'] == ['USER']
        request(f'/users/{user_id}/block', 'PATCH', token=token)
        listed = request('/users?page=1&size=100', token=token)['data']['data']
        assert next(u for u in listed if u['id'] == user_id)['status'] == 'BLOCKED'
        request('/auth/login', 'POST', {'username': email, 'password': password}, expected=400)
        request(f'/users/{user_id}/unblock', 'PATCH', token=token)
        member_token = request('/auth/login', 'POST', {'username': email, 'password': password})['data']['accessToken']
        request(f'/users/{user_id}/roles', 'POST', {'role': 'SUPER_ADMIN'}, member_token, 403)
        request(f'/users/{user_id}', 'DELETE', token=token)
        listed = request('/users?page=1&size=100', token=token)['data']['data']
        assert next(u for u in listed if u['id'] == user_id)['status'] == 'DELETED'
        request('/auth/login', 'POST', {'username': email, 'password': password}, expected=400)
        print('PASS: create, list, edit, assign/remove role, block, unblock, soft-delete, denied login')
    finally:
        if created is not None:
            user_id = created['id']
            try:
                request(f'/users/{user_id}', 'DELETE', token=token)
            except Exception as error:
                print('WARNING: disposable account cleanup failed:', error)


if __name__ == '__main__':
    main()
