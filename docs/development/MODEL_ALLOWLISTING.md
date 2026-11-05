# Model Allowlisting

## Contract

`ProviderModelPolicy` is an exact immutable provider→model map. `mockOnly()` allows only `mock`/`deterministic-v1`.

- No wildcard, prefix or syntax-only authorization.
- Unknown provider/model or missing entry fails closed.
- No model discovery, substitution or fallback.
- Checked at preview before a preview is registered and again in `ProviderRouter`.
- A model name that passes the syntax regex is still unauthorized unless explicitly listed.

## Adding a model/provider

Requires an explicit code/config change, review, tests and policy/approval implications:

1. register the provider explicitly;
2. add its exact model ID(s) to the policy;
3. ensure approval/credential/sandbox controls apply;
4. verify no wildcard or default expansion;
5. run full quality/security gates.

## Tests

Exact mock match, unknown provider/model, empty policy, no prefix/wildcard, immutable map, router rejection with zero provider calls and API 400 for a non-allowlisted model.
