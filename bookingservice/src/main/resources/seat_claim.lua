-- Checkout renews only existing locks owned by this user, all seats or none.
for _, key in ipairs(KEYS) do
    local owner = redis.call('GET', key)
    if owner ~= ARGV[1] then return 0 end
end
for _, key in ipairs(KEYS) do
    redis.call('SET', key, ARGV[1], 'EX', ARGV[2])
end
return 1
