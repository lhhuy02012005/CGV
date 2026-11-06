-- Compare owner and delete in one operation; never delete a replacement owner's lock.
local released = {}
for _, key in ipairs(KEYS) do
    local owner = redis.call('GET', key)
    if owner == ARGV[1] then
        redis.call('DEL', key)
        table.insert(released, key)
    end
end
return released
