# Lot Analytics Endpoints - Frontend Documentation

## Overview
These 3 new endpoints provide lot-specific analytics data for the Lot Analytics Page.

---

## 1️⃣ GET Lot Summary Analytics
**Endpoint:** `GET /api/v1/analytics/lots/{lotId}/summary`  
**Base URL:** `http://localhost:8080/api/v1/analytics/lots/{lotId}/summary`

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/summary" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json"
```

### Path Parameters
- `lotId` (UUID, required): The parking lot ID to analyze

### Response (200 OK)
```json
{
  "lotId": "7b88885d-1234-5678-abcd-ef0123456789",
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

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| `lotId` | UUID | The parking lot ID |
| `lotName` | String | Lot name |
| `totalSpots` | int | Total parking spots |
| `availableSpots` | int | Currently available spots |
| `currentOccupancyRate` | Double | Current occupancy percentage (0-100) |
| `todayRevenue` | BigDecimal | Today's total revenue in INR |
| `todayTransactionCount` | Long | Total transactions today |
| `averageParkingDurationMinutes` | Long | Average parking duration |
| `peakOccupancyRate` | Double | Highest occupancy recorded for this lot |
| `generatedAt` | DateTime | When this summary was calculated |

### Error Responses
- `403 Forbidden`: If manager trying to access another lot (managers can only see their own)
- `404 Not Found`: Lot doesn't exist
- `401 Unauthorized`: Invalid JWT

---

## 2️⃣ GET Lot Revenue Trend
**Endpoint:** `GET /api/v1/analytics/lots/{lotId}/revenue`  
**Base URL:** `http://localhost:8080/api/v1/analytics/lots/{lotId}/revenue?period=WEEKLY`

### Request
```bash
# Weekly revenue
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/revenue?period=WEEKLY" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Monthly revenue
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/revenue?period=MONTHLY" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Daily revenue
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/revenue?period=DAILY" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Query Parameters
| Parameter | Type | Default | Options |
|-----------|------|---------|---------|
| `period` | String | WEEKLY | DAILY, WEEKLY, MONTHLY |

### Response (200 OK) - Weekly
```json
{
  "lotId": "7b88885d-1234-5678-abcd-ef0123456789",
  "period": "WEEKLY",
  "totalRevenue": 87500.75,
  "currency": "INR",
  "transactionCount": 595,
  "periodStart": "2026-04-05T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "computedAt": "2026-04-12T15:30:45"
}
```

### Response (200 OK) - Monthly
```json
{
  "lotId": "7b88885d-1234-5678-abcd-ef0123456789",
  "period": "MONTHLY",
  "totalRevenue": 375000.50,
  "currency": "INR",
  "transactionCount": 2450,
  "periodStart": "2026-03-13T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "computedAt": "2026-04-12T15:30:45"
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| `lotId` | UUID | The parking lot ID |
| `period` | String | Time period (DAILY/WEEKLY/MONTHLY) |
| `totalRevenue` | BigDecimal | Total revenue in INR |
| `currency` | String | Currency (always "INR") |
| `transactionCount` | Long | Number of transactions |
| `periodStart` | DateTime | When the period started |
| `periodEnd` | DateTime | When the period ends (current time) |
| `computedAt` | DateTime | When calculation was performed |

### Period Breakdown
- **DAILY**: Last 24 hours (00:00 today → now)
- **WEEKLY**: Last 7 days (7 days ago → now)
- **MONTHLY**: Last 30 days (30 days ago → now)

---

## 3️⃣ GET Lot Occupancy Trend
**Endpoint:** `GET /api/v1/analytics/lots/{lotId}/occupancy`  
**Base URL:** `http://localhost:8080/api/v1/analytics/lots/{lotId}/occupancy?period=WEEKLY`

### Request
```bash
# Weekly occupancy
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/occupancy?period=WEEKLY" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Monthly occupancy with hourly breakdown
curl -X GET "http://localhost:8080/api/v1/analytics/lots/7b88885d-1234-5678-abcd-ef0123456789/occupancy?period=MONTHLY" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Query Parameters
| Parameter | Type | Default | Options |
|-----------|------|---------|---------|
| `period` | String | WEEKLY | DAILY, WEEKLY, MONTHLY |

### Response (200 OK)
```json
{
  "lotId": "7b88885d-1234-5678-abcd-ef0123456789",
  "period": "WEEKLY",
  "averageOccupancyRate": 72.5,
  "peakOccupancyRate": 96.8,
  "minOccupancyRate": 15.2,
  "totalSpots": 500,
  "averageAvailableSpots": 138,
  "periodStart": "2026-04-05T00:00:00",
  "periodEnd": "2026-04-12T15:30:45",
  "hourlyBreakdown": [
    {
      "hour": 0,
      "averageOccupancyRate": 22.5
    },
    {
      "hour": 1,
      "averageOccupancyRate": 18.3
    },
    {
      "hour": 2,
      "averageOccupancyRate": 15.2
    },
    {
      "hour": 6,
      "averageOccupancyRate": 25.8
    },
    {
      "hour": 9,
      "averageOccupancyRate": 88.5
    },
    {
      "hour": 10,
      "averageOccupancyRate": 92.3
    },
    {
      "hour": 11,
      "averageOccupancyRate": 95.1
    },
    {
      "hour": 12,
      "averageOccupancyRate": 96.8
    },
    {
      "hour": 13,
      "averageOccupancyRate": 94.2
    },
    {
      "hour": 14,
      "averageOccupancyRate": 92.5
    },
    {
      "hour": 18,
      "averageOccupancyRate": 78.3
    },
    {
      "hour": 19,
      "averageOccupancyRate": 65.1
    },
    {
      "hour": 20,
      "averageOccupancyRate": 52.4
    },
    {
      "hour": 23,
      "averageOccupancyRate": 28.9
    }
  ],
  "computedAt": "2026-04-12T15:30:45"
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| `lotId` | UUID | The parking lot ID |
| `period` | String | Time period (DAILY/WEEKLY/MONTHLY) |
| `averageOccupancyRate` | Double | Mean occupancy percentage across period |
| `peakOccupancyRate` | Double | Highest occupancy recorded |
| `minOccupancyRate` | Double | Lowest occupancy recorded |
| `totalSpots` | int | Total parking spots |
| `averageAvailableSpots` | int | Average free spots during period |
| `hourlyBreakdown` | Array | 24-hour occupancy breakdown (hours 0-23) |
| `periodStart` | DateTime | When period started |
| `periodEnd` | DateTime | When period ends (current time) |
| `computedAt` | DateTime | When calculation was performed |

### Hourly Breakdown Structure
```json
{
  "hour": 0,                           // Hour (0-23, 24-hour format)
  "averageOccupancyRate": 22.5        // Average occupancy for that hour
}
```

---

## Authentication & Authorization

### Required Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Access Control
- **Managers:** Can only access their own lots
- **Admins:** Can access all lots

### Error on Access Denied
```json
{
  "status": 403,
  "message": "Access denied: lot does not belong to this manager"
}
```

---

## Error Response Codes

| Status | Error | Reason |
|--------|-------|--------|
| `200` | OK | Successful response |
| `400` | Bad Request | Invalid period parameter |
| `401` | Unauthorized | Missing/invalid JWT |
| `403` | Forbidden | Manager accessing another's lot |
| `404` | Not Found | Lot doesn't exist |
| `500` | Internal Error | Server error |

### Example Error Response
```json
{
  "status": 404,
  "message": "Lot not found",
  "timestamp": "2026-04-12T15:30:45"
}
```

---

## Frontend Integration Example

### React Implementation
```javascript
// Get lot summary
const getLotSummary = async (lotId, token) => {
  const response = await fetch(
    `http://localhost:8080/api/v1/analytics/lots/${lotId}/summary`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return response.json();
};

// Get revenue trend
const getLotRevenueTrend = async (lotId, period = 'WEEKLY', token) => {
  const response = await fetch(
    `http://localhost:8080/api/v1/analytics/lots/${lotId}/revenue?period=${period}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return response.json();
};

// Get occupancy trend
const getLotOccupancyTrend = async (lotId, period = 'WEEKLY', token) => {
  const response = await fetch(
    `http://localhost:8080/api/v1/analytics/lots/${lotId}/occupancy?period=${period}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return response.json();
};

// Usage
const lotId = '7b88885d-1234-5678-abcd-ef0123456789';
const token = 'your_jwt_token';

const summary = await getLotSummary(lotId, token);
const revenueTrend = await getLotRevenueTrend(lotId, 'WEEKLY', token);
const occupancyTrend = await getLotOccupancyTrend(lotId, 'MONTHLY', token);
```

---

## Rate Limiting & Performance

- No rate limiting currently implemented
- Recommended caching: 5-10 seconds
- Average response time: 100-500ms

---

## API Status Endpoint
Check service status:
```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "UP"
}
```
