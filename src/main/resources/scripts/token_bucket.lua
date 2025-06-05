-- token_bucket.lua
-- KEYS[1] = token_key (ör: rate:bucket:user123)
-- KEYS[2] = last_refill_key (ör: rate:bucket:user123:lastRefill)
-- ARGV[1] = now (ms)
-- ARGV[2] = refill_interval (ms)
-- ARGV[3] = max_tokens
-- ARGV[4] = refill_amount

local token_key = KEYS[1]
local last_refill_key = KEYS[2]

local now = tonumber(ARGV[1])
local refill_interval = tonumber(ARGV[2])
local max_tokens = tonumber(ARGV[3])
local refill_amount = tonumber(ARGV[4])

local last_refill = tonumber(redis.call("GET", last_refill_key) or 0)
local current_tokens = tonumber(redis.call("GET", token_key) or max_tokens)

-- Refill Logic
local elapsed = now - last_refill
if elapsed >= refill_interval then
    local add = math.floor(elapsed / refill_interval) * refill_amount
    current_tokens = math.min(current_tokens + add, max_tokens)
    last_refill = now
end

-- Consume Logic
if current_tokens > 0 then
    current_tokens = current_tokens - 1
    redis.call("SET", token_key, current_tokens)
    redis.call("SET", last_refill_key, last_refill)
    return {1, current_tokens}
else
    return {0, 0}
end
