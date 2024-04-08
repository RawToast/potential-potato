## Thoughts & Plan

- Initially just tried prodding the API to see what it returned

  - As expected random 500 errors and the given structure
  - Rate limit is 1000 requests per day according to dockerhub, we can restrict the service using a cache (~86.4 seconds)
    - It's possible we could get away with a shorter cache length 60 seconds, but I'll stick with 86.4 assuming this service is constantly in use.

- Metals/VsCode doesn't handle the imports too well, so I'll need to quickly update the scalafmt config for this (and avoid making any other changes)
- Dependencies/SBT/Scala version is a little old. Updating that to the latest 2.13.x would nice

  - Metals raises a warning about the Scala version being deprecated, so it's probably a good idea to update that as I am using Metals!
  - Easy to revert this if any issues arise
  - Scala 3 is a pain due to PureConfig. It's doable, but I don't want to spend the time!
  - Pretty sure sbt-revolver isn't required on modern versions of SBT, so I'll remove that

- Add basic tests to cover the existing code
- Introduce client trait for the "Kinds" call

  - Test drive a client implementation
  - Wire the client into the existing service and update the test

- Add get instance data call to the client

  - Update the service / route & tests

- Add another client for the 'Pricing' part. This needs an auth header and to handle RateLimit (the docs say the limit only applies to this call)
- Same steps, test drive an implementation and wire it in.
- Add cacheing to the Pricing client, this can just be an Map held in memory. Probably just start with a mutable map, test that and then use Ref

- Add an easy way to run things

  - SBT assembly should build a docker image without much work
  - A compose setup would then be rather simple to implement

## Assumptions

- Avoid adding too many additional dependencies
  - Only added Ember Client alongside circe literal for testing
- As mentioned above, restricting the call limit to avoid hitting the rate limit is sufficient
- I should avoid adding additional services
  - Redis would be better for holding the cache (this would avoid different services holding different cached values)
