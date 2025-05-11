# API Documentation

This document provides detailed information about the API endpoints exposed by the Core Banking Card Transaction Authorization Center.

## Table of Contents

- [Overview](#overview)
- [Base URL](#base-url)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Idempotency](#idempotency)
- [Authorization Endpoints](#authorization-endpoints)
  - [Process Authorization](#process-authorization)
  - [Get Authorization Decision](#get-authorization-decision)
  - [Get Decision by Request ID](#get-decision-by-request-id)
  - [Reverse Authorization](#reverse-authorization)
  - [Complete 3DS Challenge](#complete-3ds-challenge)
- [Hold Management Endpoints](#hold-management-endpoints)
  - [Get Hold](#get-hold)
  - [Capture Hold](#capture-hold)
  - [Release Hold](#release-hold)
  - [List Holds](#list-holds)
- [Administrative Endpoints](#administrative-endpoints)
  - [Get Card Limits](#get-card-limits)
  - [Update Card Limits](#update-card-limits)
  - [Create Limit Override](#create-limit-override)
  - [Get Limit Overrides](#get-limit-overrides)
  - [Delete Limit Override](#delete-limit-override)
  - [Get Spending Summary](#get-spending-summary)
- [Webhook Notifications](#webhook-notifications)
- [API Versioning](#api-versioning)

## Overview

The Core Banking Card Transaction Authorization Center provides a RESTful API for processing card transactions, managing authorization holds, and administering the system. The API follows standard HTTP conventions and uses JSON for request and response payloads.

## Base URL

All API endpoints are relative to the base URL:

```
https://api.example.com/card-authorization
```

For development environments, the base URL is typically:

```
http://localhost:8080
```

## Authentication

The API uses OAuth 2.0 for authentication. Clients must include a valid access token in the `Authorization` header of each request:

```
Authorization: Bearer <access-token>
```

Access tokens can be obtained through the authentication service using client credentials or other OAuth 2.0 flows.

## Error Handling

The API returns standard HTTP status codes to indicate the success or failure of a request. In case of an error, the response body includes additional information about the error:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid transaction amount",
  "path": "/api/v1/authorizations",
  "timestamp": "2023-05-01T14:30:00Z",
  "details": {
    "field": "amount",
    "value": "-100.00",
    "constraint": "must be greater than 0"
  }
}
```

Common error status codes:

- `400 Bad Request`: The request is malformed or contains invalid data
- `401 Unauthorized`: Authentication is required or has failed
- `403 Forbidden`: The authenticated user does not have permission to access the resource
- `404 Not Found`: The requested resource does not exist
- `409 Conflict`: The request conflicts with the current state of the resource
- `422 Unprocessable Entity`: The request is well-formed but cannot be processed (e.g., a declined transaction)
- `429 Too Many Requests`: The client has sent too many requests in a given time period
- `500 Internal Server Error`: An unexpected error occurred on the server

## Rate Limiting

The API implements rate limiting to protect against abuse. Rate limits are applied per client and are specified in the response headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1620000000
```

If a client exceeds the rate limit, the API returns a `429 Too Many Requests` response.

## Idempotency

To ensure that operations are not accidentally performed multiple times, the API supports idempotency for non-idempotent operations (POST, PUT, DELETE). Clients can include an `Idempotency-Key` header with a unique value:

```
Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000
```

If the same idempotency key is used for multiple identical requests, the API will return the same response for each request without performing the operation multiple times.

## Authorization Endpoints

### Process Authorization

Process a new authorization request.

**Endpoint:** `POST /api/v1/authorizations`

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "maskedPan": "411111******1111",
  "panHash": "a1b2c3d4e5f6g7h8i9j0",
  "expiryDate": "12/25",
  "merchantId": "MERCH123456",
  "merchantName": "Example Merchant",
  "channel": "POS",
  "mcc": "5411",
  "countryCode": "USA",
  "transactionType": "PURCHASE",
  "amount": 125.50,
  "currency": "USD",
  "timestamp": "2023-05-01T14:30:00Z"
}
```

**Response Status:**
- `200 OK`: Transaction approved
- `202 Accepted`: Transaction requires additional verification (3DS challenge)
- `422 Unprocessable Entity`: Transaction declined
- `400 Bad Request`: Invalid request
- `500 Internal Server Error`: Server error

**Response Body (Approved):**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "APPROVED",
  "reasonCode": "APPROVED_TRANSACTION",
  "reasonMessage": "Transaction approved",
  "approvedAmount": 125.50,
  "currency": "USD",
  "authorizationCode": "123456",
  "riskScore": 25,
  "holdId": 456789123,
  "timestamp": "2023-05-01T14:30:05Z"
}
```

**Response Body (Declined):**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "DECLINED",
  "reasonCode": "INSUFFICIENT_FUNDS",
  "reasonMessage": "Insufficient funds",
  "approvedAmount": 0.00,
  "currency": "USD",
  "riskScore": 25,
  "timestamp": "2023-05-01T14:30:05Z"
}
```

**Response Body (Challenge):**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "CHALLENGE",
  "reasonCode": "ADDITIONAL_AUTHENTICATION_REQUIRED",
  "reasonMessage": "Additional authentication required",
  "approvedAmount": 125.50,
  "currency": "USD",
  "riskScore": 75,
  "challengeUrl": "https://3ds.example.com/challenge/123456789",
  "timestamp": "2023-05-01T14:30:05Z"
}
```

### Get Authorization Decision

Retrieve an authorization decision by its ID.

**Endpoint:** `GET /api/v1/authorizations/{decisionId}`

**Path Parameters:**
- `decisionId`: The ID of the authorization decision

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Decision found
- `404 Not Found`: Decision not found
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "APPROVED",
  "reasonCode": "APPROVED_TRANSACTION",
  "reasonMessage": "Transaction approved",
  "approvedAmount": 125.50,
  "currency": "USD",
  "authorizationCode": "123456",
  "riskScore": 25,
  "holdId": 456789123,
  "timestamp": "2023-05-01T14:30:05Z"
}
```

### Get Decision by Request ID

Retrieve an authorization decision by the request ID.

**Endpoint:** `GET /api/v1/authorizations/request/{requestId}`

**Path Parameters:**
- `requestId`: The ID of the authorization request

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Decision found
- `404 Not Found`: Decision not found
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "APPROVED",
  "reasonCode": "APPROVED_TRANSACTION",
  "reasonMessage": "Transaction approved",
  "approvedAmount": 125.50,
  "currency": "USD",
  "authorizationCode": "123456",
  "riskScore": 25,
  "holdId": 456789123,
  "timestamp": "2023-05-01T14:30:05Z"
}
```

### Reverse Authorization

Reverse a previously approved authorization.

**Endpoint:** `POST /api/v1/authorizations/{requestId}/reverse`

**Path Parameters:**
- `requestId`: The ID of the authorization request to reverse

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "reason": "Merchant initiated reversal"
}
```

**Response Status:**
- `200 OK`: Authorization reversed successfully
- `404 Not Found`: Authorization not found
- `409 Conflict`: Authorization cannot be reversed (e.g., already captured)
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "DECLINED",
  "reasonCode": "AUTHORIZATION_REVERSED",
  "reasonMessage": "Authorization reversed: Merchant initiated reversal",
  "approvedAmount": 0.00,
  "currency": "USD",
  "riskScore": 25,
  "timestamp": "2023-05-01T15:30:05Z"
}
```

### Complete 3DS Challenge

Complete a 3D Secure challenge for an authorization.

**Endpoint:** `POST /api/v1/authorizations/{requestId}/challenge-complete`

**Path Parameters:**
- `requestId`: The ID of the authorization request

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "authenticationResult": "SUCCESS",
  "authenticationValue": "3ds-authentication-value",
  "eci": "05",
  "cavv": "cavv-value",
  "xid": "xid-value"
}
```

**Response Status:**
- `200 OK`: Challenge completed successfully
- `404 Not Found`: Authorization not found
- `409 Conflict`: Challenge cannot be completed (e.g., not in CHALLENGE state)
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "requestId": 123456789,
  "decisionId": 987654321,
  "decision": "APPROVED",
  "reasonCode": "APPROVED_TRANSACTION",
  "reasonMessage": "Transaction approved",
  "approvedAmount": 125.50,
  "currency": "USD",
  "authorizationCode": "123456",
  "riskScore": 25,
  "holdId": 456789123,
  "timestamp": "2023-05-01T14:35:05Z"
}
```

## Hold Management Endpoints

### Get Hold

Retrieve an authorization hold by its ID.

**Endpoint:** `GET /api/v1/holds/{holdId}`

**Path Parameters:**
- `holdId`: The ID of the authorization hold

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Hold found
- `404 Not Found`: Hold not found
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "holdId": 456789123,
  "requestId": 123456789,
  "cardId": 789123456,
  "accountId": 321654987,
  "accountSpaceId": 654987321,
  "amount": 125.50,
  "capturedAmount": 0.00,
  "currency": "USD",
  "merchantId": "MERCH123456",
  "merchantName": "Example Merchant",
  "status": "ACTIVE",
  "createdAt": "2023-05-01T14:30:05Z",
  "updatedAt": null,
  "expiresAt": "2023-05-08T14:30:05Z"
}
```

### Capture Hold

Capture an authorization hold.

**Endpoint:** `POST /api/v1/holds/{holdId}/capture`

**Path Parameters:**
- `holdId`: The ID of the authorization hold to capture

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "amount": 125.50,
  "currency": "USD",
  "reference": "SETTLEMENT-123456"
}
```

**Response Status:**
- `200 OK`: Hold captured successfully
- `404 Not Found`: Hold not found
- `409 Conflict`: Hold cannot be captured (e.g., not in ACTIVE state)
- `400 Bad Request`: Invalid capture amount
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "holdId": 456789123,
  "requestId": 123456789,
  "cardId": 789123456,
  "accountId": 321654987,
  "accountSpaceId": 654987321,
  "amount": 125.50,
  "capturedAmount": 125.50,
  "currency": "USD",
  "merchantId": "MERCH123456",
  "merchantName": "Example Merchant",
  "status": "CAPTURED",
  "createdAt": "2023-05-01T14:30:05Z",
  "updatedAt": "2023-05-02T10:15:00Z",
  "expiresAt": "2023-05-08T14:30:05Z"
}
```

### Release Hold

Release an authorization hold.

**Endpoint:** `POST /api/v1/holds/{holdId}/release`

**Path Parameters:**
- `holdId`: The ID of the authorization hold to release

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "reason": "Merchant initiated release"
}
```

**Response Status:**
- `200 OK`: Hold released successfully
- `404 Not Found`: Hold not found
- `409 Conflict`: Hold cannot be released (e.g., not in ACTIVE state)
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "accountId": 321654987,
  "accountSpaceId": 654987321,
  "availableBalance": 5000.00,
  "reservedBalance": 0.00,
  "postedBalance": 4500.00,
  "currency": "USD",
  "timestamp": "2023-05-02T10:20:00Z"
}
```

### List Holds

List authorization holds for an account.

**Endpoint:** `GET /api/v1/holds`

**Query Parameters:**
- `accountId`: (required) The ID of the account
- `status`: (optional) Filter by hold status (ACTIVE, CAPTURED, RELEASED, EXPIRED)
- `fromDate`: (optional) Filter by creation date (ISO 8601 format)
- `toDate`: (optional) Filter by creation date (ISO 8601 format)
- `page`: (optional) Page number (default: 0)
- `size`: (optional) Page size (default: 20)

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Holds retrieved successfully
- `400 Bad Request`: Invalid query parameters
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "content": [
    {
      "holdId": 456789123,
      "requestId": 123456789,
      "cardId": 789123456,
      "accountId": 321654987,
      "accountSpaceId": 654987321,
      "amount": 125.50,
      "capturedAmount": 0.00,
      "currency": "USD",
      "merchantId": "MERCH123456",
      "merchantName": "Example Merchant",
      "status": "ACTIVE",
      "createdAt": "2023-05-01T14:30:05Z",
      "updatedAt": null,
      "expiresAt": "2023-05-08T14:30:05Z"
    },
    {
      "holdId": 456789124,
      "requestId": 123456790,
      "cardId": 789123456,
      "accountId": 321654987,
      "accountSpaceId": 654987321,
      "amount": 75.25,
      "capturedAmount": 75.25,
      "currency": "USD",
      "merchantId": "MERCH789012",
      "merchantName": "Another Merchant",
      "status": "CAPTURED",
      "createdAt": "2023-04-30T09:15:00Z",
      "updatedAt": "2023-05-01T10:30:00Z",
      "expiresAt": "2023-05-07T09:15:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

## Administrative Endpoints

### Get Card Limits

Retrieve the current limits for a card.

**Endpoint:** `GET /api/v1/admin/cards/{cardId}/limits`

**Path Parameters:**
- `cardId`: The ID of the card

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Limits retrieved successfully
- `404 Not Found`: Card not found
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "cardId": 789123456,
  "transactionLimit": 2000.00,
  "dailyLimit": 5000.00,
  "monthlyLimit": 20000.00,
  "channelLimits": {
    "ATM": {
      "transactionLimit": 500.00,
      "dailyLimit": 1000.00
    },
    "E_COMMERCE": {
      "transactionLimit": 1000.00,
      "dailyLimit": 3000.00
    },
    "POS": {
      "transactionLimit": 2000.00,
      "dailyLimit": 5000.00
    }
  },
  "lastUpdated": "2023-04-15T10:30:00Z"
}
```

### Update Card Limits

Update the limits for a card.

**Endpoint:** `PUT /api/v1/admin/cards/{cardId}/limits`

**Path Parameters:**
- `cardId`: The ID of the card

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "transactionLimit": 2000.00,
  "dailyLimit": 5000.00,
  "monthlyLimit": 20000.00,
  "channelLimits": {
    "ATM": {
      "transactionLimit": 500.00,
      "dailyLimit": 1000.00
    },
    "E_COMMERCE": {
      "transactionLimit": 1000.00,
      "dailyLimit": 3000.00
    }
  }
}
```

**Response Status:**
- `200 OK`: Limits updated successfully
- `404 Not Found`: Card not found
- `400 Bad Request`: Invalid limits
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "cardId": 789123456,
  "transactionLimit": 2000.00,
  "dailyLimit": 5000.00,
  "monthlyLimit": 20000.00,
  "channelLimits": {
    "ATM": {
      "transactionLimit": 500.00,
      "dailyLimit": 1000.00
    },
    "E_COMMERCE": {
      "transactionLimit": 1000.00,
      "dailyLimit": 3000.00
    },
    "POS": {
      "transactionLimit": 2000.00,
      "dailyLimit": 5000.00
    }
  },
  "lastUpdated": "2023-05-02T11:45:00Z"
}
```

### Create Limit Override

Create a temporary limit override for a card.

**Endpoint:** `POST /api/v1/admin/cards/{cardId}/limit-overrides`

**Path Parameters:**
- `cardId`: The ID of the card

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`
- `Idempotency-Key: <idempotency-key>` (optional)

**Request Body:**
```json
{
  "transactionLimit": 5000.00,
  "dailyLimit": 10000.00,
  "monthlyLimit": 30000.00,
  "reason": "Customer traveling abroad",
  "expiresAt": "2023-05-15T23:59:59Z"
}
```

**Response Status:**
- `201 Created`: Limit override created successfully
- `404 Not Found`: Card not found
- `400 Bad Request`: Invalid override
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "id": 123456,
  "cardId": 789123456,
  "transactionLimit": 5000.00,
  "dailyLimit": 10000.00,
  "monthlyLimit": 30000.00,
  "reason": "Customer traveling abroad",
  "createdBy": "admin-user",
  "createdAt": "2023-05-02T11:50:00Z",
  "expiresAt": "2023-05-15T23:59:59Z"
}
```

### Get Limit Overrides

Retrieve the active limit overrides for a card.

**Endpoint:** `GET /api/v1/admin/cards/{cardId}/limit-overrides`

**Path Parameters:**
- `cardId`: The ID of the card

**Query Parameters:**
- `includeExpired`: (optional) Include expired overrides (default: false)
- `page`: (optional) Page number (default: 0)
- `size`: (optional) Page size (default: 20)

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Overrides retrieved successfully
- `404 Not Found`: Card not found
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "content": [
    {
      "id": 123456,
      "cardId": 789123456,
      "transactionLimit": 5000.00,
      "dailyLimit": 10000.00,
      "monthlyLimit": 30000.00,
      "reason": "Customer traveling abroad",
      "createdBy": "admin-user",
      "createdAt": "2023-05-02T11:50:00Z",
      "expiresAt": "2023-05-15T23:59:59Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

### Delete Limit Override

Delete a limit override.

**Endpoint:** `DELETE /api/v1/admin/limit-overrides/{overrideId}`

**Path Parameters:**
- `overrideId`: The ID of the limit override

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `204 No Content`: Override deleted successfully
- `404 Not Found`: Override not found
- `500 Internal Server Error`: Server error

### Get Spending Summary

Retrieve a summary of spending for a card.

**Endpoint:** `GET /api/v1/admin/cards/{cardId}/spending-summary`

**Path Parameters:**
- `cardId`: The ID of the card

**Query Parameters:**
- `fromDate`: (optional) Start date for the summary (ISO 8601 format)
- `toDate`: (optional) End date for the summary (ISO 8601 format)

**Request Headers:**
- `Authorization: Bearer <access-token>`

**Response Status:**
- `200 OK`: Summary retrieved successfully
- `404 Not Found`: Card not found
- `400 Bad Request`: Invalid date range
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "cardId": 789123456,
  "fromDate": "2023-05-01T00:00:00Z",
  "toDate": "2023-05-02T23:59:59Z",
  "totalSpent": 200.75,
  "transactionCount": 2,
  "byChannel": {
    "POS": {
      "totalSpent": 125.50,
      "transactionCount": 1
    },
    "E_COMMERCE": {
      "totalSpent": 75.25,
      "transactionCount": 1
    }
  },
  "byMerchantCategory": {
    "5411": {
      "totalSpent": 125.50,
      "transactionCount": 1
    },
    "5732": {
      "totalSpent": 75.25,
      "transactionCount": 1
    }
  },
  "dailySpending": [
    {
      "date": "2023-05-01",
      "totalSpent": 125.50,
      "transactionCount": 1
    },
    {
      "date": "2023-05-02",
      "totalSpent": 75.25,
      "transactionCount": 1
    }
  ]
}
```

## Webhook Notifications

The system can send webhook notifications for various events:

1. **Authorization Decision**: Sent when an authorization decision is made
2. **Hold Status Change**: Sent when a hold status changes (e.g., captured, released, expired)
3. **Limit Breach**: Sent when a transaction is declined due to a limit breach

To configure webhooks, use the administrative API:

**Endpoint:** `POST /api/v1/admin/webhooks`

**Request Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <access-token>`

**Request Body:**
```json
{
  "url": "https://example.com/webhooks/card-authorization",
  "events": ["AUTHORIZATION_DECISION", "HOLD_STATUS_CHANGE", "LIMIT_BREACH"],
  "secret": "webhook-secret-key"
}
```

**Response Status:**
- `201 Created`: Webhook created successfully
- `400 Bad Request`: Invalid webhook configuration
- `500 Internal Server Error`: Server error

**Response Body:**
```json
{
  "id": "webhook-123456",
  "url": "https://example.com/webhooks/card-authorization",
  "events": ["AUTHORIZATION_DECISION", "HOLD_STATUS_CHANGE", "LIMIT_BREACH"],
  "active": true,
  "createdAt": "2023-05-02T12:00:00Z"
}
```

## API Versioning

The API is versioned through the URL path. The current version is `v1`:

```
/api/v1/authorizations
```

Future versions will be available at:

```
/api/v2/authorizations
```

When a new version is released, the previous version will be supported for a deprecation period, typically 6-12 months. Deprecation notices will be provided through response headers and documentation.
