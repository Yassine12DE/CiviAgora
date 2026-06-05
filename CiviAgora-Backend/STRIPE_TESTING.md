# Stripe Test Checkout

This backend integration is test-mode only.

## Required environment variables

- `STRIPE_SECRET_KEY` must start with `sk_test_`
- `STRIPE_PUBLISHABLE_KEY` must start with `pk_test_` if you expose it to the frontend
- `STRIPE_WEBHOOK_SECRET` must start with `whsec_`
- `STRIPE_SUCCESS_URL` defaults to `http://lvh.me:5173/stripe/success`
- `STRIPE_CANCEL_URL` defaults to `http://lvh.me:5173/stripe/cancel`
- `STRIPE_CURRENCY` defaults to `usd`

## Demo flows

- Organization request payment link: open `/payment/:token`, then click the Stripe button.
- SaaS subscription demo: use the Stripe checkout button in the `Plans & Subscriptions` page.
- Back-office validation: use the Stripe checkout link from the organization request detail panel.
- Live updates are delivered by SSE:
  - SaaS stream: `GET /saas/organization-requests/events`
  - Public token stream: `GET /public/organization-requests/{paymentToken}/events`
- The webhook remains the source of truth. The UI only reacts after the backend confirms the payment.

## Test card

Use the following Stripe test card in hosted Checkout:

- `4242 4242 4242 4242`
- any future expiry date
- any CVC

## Webhook

Configure a webhook endpoint at `/public/stripe/webhook` and verify it with `STRIPE_WEBHOOK_SECRET`.
The backend handles `checkout.session.completed` and marks the linked request as paid when the session originated from an organization request.

## Stripe CLI

Use the Stripe CLI to forward local webhook events:

```bash
stripe listen --forward-to localhost:8081/public/stripe/webhook
```

If the webhook is unavailable locally, you can use the safe demo refresh endpoint:

```bash
POST /public/stripe/checkout-sessions/{sessionId}/refresh
```

That endpoint re-reads the Stripe session with the backend secret key and synchronizes the stored request state for testing only.
