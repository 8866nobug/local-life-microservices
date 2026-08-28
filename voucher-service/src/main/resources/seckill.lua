-- 秒杀资格校验 + 扣减库存（在 Redis 内原子执行）
-- KEYS 不使用；参数：ARGV[1]=voucherId, ARGV[2]=userId
-- 返回值：0=成功；1=库存不足；2=重复下单（一人一单）
local voucherId = ARGV[1]
local userId = ARGV[2]

-- 库存 key / 已下单用户集合 key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1. 校验库存
if (tonumber(redis.call('get', stockKey)) or 0) <= 0 then
    return 1
end

-- 2. 校验是否已下单（一人一单）
if redis.call('sismember', orderKey, userId) == 1 then
    return 2
end

-- 3. 扣减库存并登记用户
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
return 0
