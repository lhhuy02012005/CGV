-- KEYS[1]: {promo:PROMO_ID}:slots
-- KEYS[2]: {promo:PROMO_ID}:user:{USER_ID}

local userHeld = redis.call('GET', KEYS[2])
if userHeld and tonumber(userHeld) > 0 then
    local newHeld = redis.call('DECR', KEYS[2])
    if newHeld <= 0 then
        redis.call('DEL', KEYS[2])
    end
    redis.call('INCR', KEYS[1])
    return 1 -- Đã hoàn trả 1 slot thành công
end

return 0 -- Không có slot nào cần hoàn trả
