// k6-rl.js
// Usage:
// BASE_URL=http://localhost:8080/hello TENANT=acme USER_ID=u1 k6 run k6-rl.js
//
// Notlar:
// - 429, rate limiter için beklenen sonuçtur. Eşikler buna göre ayarlanmıştır.
// - "constant-arrival-rate" deterministik RPS üretir (önerilir).

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// ---- Custom Metrikler ----
export const http_200_count = new Counter('http_200_count');
export const http_429_count = new Counter('http_429_count');

// ---- Parametreler / ENV ----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/hello';
const TENANT = __ENV.TENANT || 'acme';
const USER = __ENV.USER_ID || 'u1';

// ---- Seçenekler / Senaryolar / Eşikler ----
export const options = {
  scenarios: {
    burst: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.BURST_RPS || 300), // 300 RPS
      timeUnit: '1s',
      duration: __ENV.BURST_DURATION || '10s',
      preAllocatedVUs: Number(__ENV.BURST_VUS || 50),
      maxVUs: Number(__ENV.BURST_MAX_VUS || 100),
      exec: 'hit',
    },
    steady: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.STEADY_RPS || 3), // ~3 RPS
      timeUnit: '1s',
      duration: __ENV.STEADY_DURATION || '70s',
      preAllocatedVUs: Number(__ENV.STEADY_VUS || 50),
      maxVUs: Number(__ENV.STEADY_MAX_VUS || 100),
      startTime: __ENV.STEADY_START || '10s',
      exec: 'hit',
    },
  },

  // 429'ları beklenen yap: http_req_failed metrikleri bu etiketle filtrelenecek
  tags: { expected_response: 'true' },

  // Genel limitler
  discardResponseBodies: true,
  thresholds: {
    // Gerçek hatalar (ağ hatası, 5xx vb.) ~%1'in altında olmalı
    'http_req_failed{expected_response:true}': ['rate<0.01'],

    // Burst fazında mutlaka önemli sayıda 429 üretmeliyiz (bucket dolduğunu gösterir)
    'http_429_count{scenario:burst}': ['count>100'],

    // Steady fazda sistemin 200 döndürebildiğini görmek istiyoruz
    'http_200_count{scenario:steady}': ['count>100'],

    // Steady gecikme SLO (örnek): 95. yüzdelik < 50ms
    'http_req_duration{scenario:steady}': ['p(95)<50'],
  },

  // Graceful duruş (isteğe bağlı)
  gracefulStop: '5s',
};

// ---- İstek Fonksiyonu ----
export function hit() {
  const res = http.get(BASE_URL, {
    headers: {
      'X-Tenant-Id': TENANT,
      'X-User-Id': USER,
    },
    // İstersen senaryo bazında değil istek bazında da expected tag'i ekleyebilirsin:
    // tags: { expected_response: 'true' }
  });

  // Sayaçlar (res.tags KULLANMIYORUZ)
  if (res.status === 200) http_200_count.add(1);
  else if (res.status === 429) http_429_count.add(1);

  // Yalnızca 200 veya 429'u "başarılı kabul" ediyoruz
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });

  // CPU'yu yakmamak için mini uyku
  sleep(0.01);
}
