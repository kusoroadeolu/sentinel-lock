# Draft 
A JVM client that other JVMs can connect to it to allow distributed synchronization across those JVM instances

# Core flow
Lock registry -> Client -> Lease 
Proposed communication mechanism: grpc


## Lock registry
- Responsible for holding the synchronizer and it's meta data in a map
- Allows other JVMs to request for leases to a particular synchronizer. 
A lease is a contract that the client must fulfill within a particular amount of time. 

**Registry Invariants**:
- Each synchronizer has at most one active lease at any time
- Fencing tokens are strictly monotonically increasing
- Expired leases are automatically revoked

**Client Responsibilities**:
- Must include fencing token when committing operations to storage
- Must complete work within lease duration or renew before expiration

Tech stack
Redis to store current synchronizers, leases and expiry times
An in memory concurrent queue to order incoming clients


# Api Proposal

```java

```