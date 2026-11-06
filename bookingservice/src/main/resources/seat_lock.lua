-- KEYS[1]: active-showtime key; KEYS[2..n]: actual seat keys (single Redis instance).
-- ARGV: owner, ttl seconds, showtimeId. Check and write the entire batch atomically.
local active = redis.call('GET', KEYS[1])
if active and active ~= ARGV[3] then return -1 end
for i = 2, #KEYS do
    local holder = redis.call('GET', KEYS[i])
    if holder and holder ~= ARGV[1] then return 0 end
end
for i = 2, #KEYS do
    redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])
end
redis.call('SET', KEYS[1], ARGV[3], 'EX', ARGV[2])
return 1
