# 3 New Lot Analytics APIs - Quick Reference

## Base URL
```
http://localhost:8080/api/v1/analytics
```

---

## API 1: Lot Summary Analytics
```
GET /lots/{lotId}/summary
```

### Example Request
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/summary" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Example Response (200)
```json
{
  "lotId": "7b88885d-f111-2222-3333-ef0123456789",
  "lotName": "Downtown Plaza Parking",
  "totalSpots": 500,
  "availableSpots": 120,
  "currentOccupancyRate": 76.0,
  "todayRevenue": 12500.50,
  "todayTransactionCount": 85,
  "averageParkingDurationMinutes": 127,
  "peakOccupancyRate": 95.5,
  "generatedAt": "2026-04-12T15:30:45"
}
```

---

## API 2: Lot Revenue Trend
```
GET /lots/{lotId}/revenue?period=WEEKLY|DAILY|MONTHLY
```

### Example Requests
```bash
# Weekly
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/revenue?period=WEEKLY" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Daily
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/revenue?period=DAILY" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Monthly
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/revenue?period=MONTHLY" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Example Response - Weekly (200)
```json
{
  "lotId": "7b88885d-f111-2222-3333-ef0123456789",
  "period": "WEEKLY",
  "totalRevenue": 87500.75,
  "currency": "INR",
  "transactionCount": 595,
  "periodStart": "2026-04-05T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "computedAt": "2026-04-12T15:30:45"
}
```

### Example Response - Monthly (200)
```json
{
  "lotId": "7b88885d-f111-2222-3333-ef0123456789",
  "period": "MONTHLY",
  "totalRevenue": 375000.50,
  "currency": "INR",
  "transactionCount": 2450,
  "periodStart": "2026-03-13T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "computedAt": "2026-04-12T15:30:45"
}
```

---

## API 3: Lot Occupancy Trend
```
GET /lots/{lotId}/occupancy?period=WEEKLY|DAILY|MONTHLY
```

### Example Requests
```bash
# Weekly occupancy
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/occupancy?period=WEEKLY" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Monthly occupancy
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-f111-2222-3333-ef0123456789/occupancy?period=MONTHLY" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Example Response (200)
```json
{
  "lotId": "7b88885d-f111-2222-3333-ef0123456789",
  "period": "WEEKLY",
  "averageOccupancyRate": 72.5,
  "peakOccupancyRate": 96.8,
  "minOccupancyRate": 15.2,
  "totalSpots": 500,
  "averageAvailableSpots": 138,
  "periodStart": "2026-04-05T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "hourlyBreakdown": [
    {"hour": 0, "averageOccupancyRate": 22.5},
    {"hour": 1, "averageOccupancyRate": 18.3},
    {"hour": 2, "averageOccupancyRate": 15.2},
    {"hour": 6, "averageOccupancyRate": 25.8},
    {"hour": 9, "averageOccupancyRate": 88.5},
    {"hour": 10, "averageOccupancyRate": 92.3},
    {"hour": 11, "averageOccupancyRate": 95.1},
    {"hour": 12, "averageOccupancyRate": 96.8},
    {"hour": 13, "averageOccupancyRate": 94.2},
    {"hour": 14, "averageOccupancyRate": 92.5},
    {"hour": 18, "averageOccupancyRate": 78.3},
    {"hour": 19, "averageOccupancyRate": 65.1},
    {"hour": 20, "averageOccupancyRate": 52.4},
    {"hour": 23, "averageOccupancyRate": 28.9}
  ],
  "computedAt": "2026-04-12T15:30:45"
}
```

---

## Common Parameters

### All Endpoints Require
- **Authorization Header:** `Bearer <JWT_TOKEN>`
- **Content-Type:** `application/json`

### Access Control
- **MANAGER** role → Can only access their own lots
- **ADMIN** role → Can access all lots
- **DRIVER** role → ❌ No access

### Period Parameter Values
- `DAILY` → Last 24 hours (00:00 today → now)
- `WEEKLY` → Last 7 days (default)
- `MONTHLY` → Last 30 days

---

## Error Responses

### 403 Forbidden (Manager accessing another's lot)
```json
{
  "status": 403,
  "message": "Access denied: lot does not belong to this manager"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "message": "Lot not found: <lotId>"
}
```

### 401 Unauthorized (Invalid JWT)
```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

---

## Quick Integration (JavaScript/React)

```javascript
const TOKEN = 'your_jwt_token_here';
const LOT_ID = '7b88885d-f111-2222-3333-ef0123456789';
const BASE_URL = 'http://localhost:8080/api/v1/analytics';

// Helper function
async function fetchAnalytics(endpoint, params = {}) {
  const url = new URL(`${BASE_URL}${endpoint}`);
  Object.entries(params).forEach(([key, value]) => {
    url.searchParams.append(key, value);
  });

  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`API Error: ${response.status}`);
  }

  return response.json();
}

// Usage
const summary = await fetchAnalytics(`/lots/${LOT_ID}/summary`);
const revenueTrend = await fetchAnalytics(`/lots/${LOT_ID}/revenue`, { period: 'WEEKLY' });
const occupancyTrend = await fetchAnalytics(`/lots/${LOT_ID}/occupancy`, { period: 'MONTHLY' });

console.log(summary);
console.log(revenueTrend);
console.log(occupancyTrend);
```

---

## Status Codes Summary

| Code | Meaning | When |
|------|---------|------|
| 200 | Success | Data returned |
| 400 | Bad Request | Invalid period parameter |
| 401 | Unauthorized | Missing/invalid JWT |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Lot doesn't exist |
| 500 | Server Error | Database/service error |

---

## Implementation Checklist for Frontend

- [ ] Replace hardcoded API endpoints with these 3 new endpoints
- [ ] Update period selector to use DAILY/WEEKLY/MONTHLY
- [ ] Add Authorization header to all requests
- [ ] Handle 403 error for managers accessing other lots
- [ ] Cache responses for 5-10 seconds
- [ ] Format timestamp responses with local timezone
- [ ] Display revenue in ₹ (INR currency symbol)
- [ ] Display occupancy as percentage (0-100%)
