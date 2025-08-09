-- KEYS[1]=bucketKey
-- ARGV[1]=capacity  ARGV[2]=refillTokensPerSec
-- ARGV[3]=nowMillis ARGV[4]=cost  ARGV[5]=ttlSec
local cap = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now  = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])
local ttl  = tonumber(ARGV[5])

local lastTokens = tonumber(redis.call('HGET', KEYS[1], 't') or cap)
local lastTime   = tonumber(redis.call('HGET', KEYS[1], 'ts') or now)
local delta = math.max(0, now - lastTime)
local refill = (delta/1000.0) * rate
local tokens = math.min(cap, lastTokens + refill)

local allowed = tokens >= cost
if allowed then
  tokens = tokens - cost
  redis.call('HSET', KEYS[1], 't', tokens, 'ts', now)
  redis.call('EXPIRE', KEYS[1], ttl)
  return {1, math.floor(tokens), 0}
else
  local deficit = cost - tokens
  local retryAfter = math.ceil(deficit / rate)
  redis.call('HSET', KEYS[1], 't', tokens, 'ts', now)
  redis.call('EXPIRE', KEYS[1], ttl)
  return {0, math.floor(tokens), retryAfter}
end
