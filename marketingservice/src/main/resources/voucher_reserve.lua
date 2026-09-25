-- KEYS[1]: {promo:PROMO_ID}:slots (Key lưu số slot còn lại)
-- KEYS[2]: {promo:PROMO_ID}:user:{USER_ID} (Key lưu số lần giữ/áp dụng của user này)
-- ARGV[1]: maxUsesPerUser (số lần tối đa user được dùng, VD: 1)
-- ARGV[2]: ttlSeconds (thời gian giữ slot, VD: 900 giây = 15 phút)
-- ARGV[3]: initialSlotsIfAbsent (số slot khởi tạo từ DB nếu key chưa có trên Redis)

-- 1. Kiểm tra xem user này đã giữ slot chưa
local userHeld = redis.call('GET', KEYS[2])
if userHeld and tonumber(userHeld) >= tonumber(ARGV[1]) then
    return -2 -- Error: User đã giữ hoặc đã dùng hết hạn mức của mình
end

-- 2. Kiểm tra xem key tổng slot đã tồn tại trên Redis chưa, nếu chưa thì khởi tạo từ DB
local currentSlots = redis.call('GET', KEYS[1])
if not currentSlots then
    currentSlots = tonumber(ARGV[3])
    redis.call('SET', KEYS[1], currentSlots)
else
    currentSlots = tonumber(currentSlots)
end

-- 3. Kiểm tra số slot còn lại
if currentSlots <= 0 then
    return -1 -- Error: Voucher đã hết lượt sử dụng (Hết slot)
end

-- 4. Trừ slot và ghi nhận phiên giữ của user
redis.call('DECR', KEYS[1])
redis.call('INCR', KEYS[2])
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))

return 1 -- Thành công giữ 1 slot
