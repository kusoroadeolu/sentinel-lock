package io.github.kusoroadeolu.sentinellock.entities;

public interface SaveResult {
        default boolean isValid(){
            return false;
        }

        record Success() implements SaveResult {
            public boolean isValid() {
                return true;
            }
        }
        enum Invalid implements SaveResult{
            INVALID_LEASE
        }
        record Failed(Cause cause) implements SaveResult{
            public enum Cause{
                ERR, RACE_CONDITION, ACQUIRED
            }
        }
    }