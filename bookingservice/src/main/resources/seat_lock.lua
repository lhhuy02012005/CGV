-- KEYS[1]: Prefix của showtime (VD: showtime:lock:UUID-suatchieu)
-- ARGV[1]: userId
-- ARGV[2]: TTL giữ ghế (giây)
-- ARGV[3]: Danh sách các seatId muốn giữ

local showtime_prefix = KEYS[1]
local user_id = ARGV[1]
local ttl = tonumber(ARGV[2])

for i = 3 , #ARGV do
    local seat_id = ARGV[i]
    local lock_key = showtime_prefix .. ":" .. seat_id
    if redis.call('EXISTS' , lock_key) == 1 then
        return 0
    end
end

for i = 3 , #ARGV do
    local seat_id = ARGV[i]
    local lock_key = showtime_prefix .. ":" .. seat_id
    redis.call('SET', lock_key , user_id , 'EX' , ttl)
end

return 1