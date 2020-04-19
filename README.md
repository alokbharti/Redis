# Implementation of Redis

Due to less time, these things are not implemented yet. So, please avoid them while executing these commands:
1. Don't use quotes ("") for values. Write the values without them
2. For now, only single words are allowed. Ex- `SET mykey hello world` will not execute while `SET mykey hello` will do


For now, only these functions are allowed:
1. **SET**: The SET command supports a set of options that modify its behavior:
    - EX seconds -- Set the specified expire time, in seconds.
    - PX milliseconds -- Set the specified expire time, in milliseconds.
    - NX -- Only set the key if it does not already exist.
    - XX -- Only set the key if it already exist.
        
            Examples: redis>SET mykey hello
                      OK
                      redis>SET mykey hi NX
                      Error
                      redis>SET mykey hey XX
                      OK
                      redis>SET mykey world EX 10
                      ok
                  
                      After 10 seconds
                      redis>GET mykey
                      Nil

2. **GET**: Get the value of key. If the key does not exist the special value nil is returned.<br>
            An error is returned if the value stored at key is not a string, because GET only handles string values.
            
            Examples: redis>GET mykey
                      Nil
                      redis>SET mykey hello
                      OK
                      redis>GET mykey
                      hello
                      
 3. **EXPIRE**: Set a timeout on key. After the timeout has expired, the key will automatically be deleted. A key with an associated                     timeout is often said to be volatile in Redis terminology.
 
                Examples: redis>SET mykey hello
                          OK
                          redis>EXPIRE mykey 5
                          1
                          
                          After 5seconds                          
                          redis>GET mykey
                          Nil
                          
 4. **[ZADD](https://redis.io/commands/zadd)**: ZADD supports a list of options, specified after the name of the key and before the first score argument. Options are:

     - XX: Only update elements that already exist. Never add elements.
     - NX: Don't update already existing elements. Always add new elements.
     - CH: Modify the return value from the number of new elements added, to the total number of elements changed (CH is an abbreviation        of -changed). Changed elements are new elements added and elements already existing for which the score was updated. So elements        specified in the command line having the same score as they had in the past are not counted. Note: normally the return value of          ZADD only counts the number of new elements added. 
     
       Examples: redis>ZADD myzset 1 a
                 1
                 redis>ZRANGE myzset 0 1 WITHSCORES
                 a
                 1
                 redis>ZADD myzset 3 c 2 b
                 2
                 redis>ZRANGE myzset 0 -1 WITHSCORES
                 a
                 1
                 b 
                 2
                 c
                 3
                 
5. **[ZRANK](https://redis.io/commands/zrank)**: If member exists in the sorted set, Integer reply: the rank of member.
If member does not exist in the sorted set or key does not exist, Bulk string reply: nil. (ranks start from 0)

            Examples: redis>ZADD myzset 1 a 3 c 2 b
                      3
                      redis>ZRANK myzset a
                      0
                      redis>ZRANK myzset b
                      2
                      redis>ZRANK myzset c
                      3
                      
6. **[ZRANGE](https://redis.io/commands/zrange)**: Returns list of elements in the specified range (optionally with their scores, in case the WITHSCORES option is given).

             Examples: redis>ZADD myzset 1 a 3 c 2 b
                       3
                       redis>ZRANGE myzset 0 3
                       a
                       b
                       c
                       redis>ZRANGE myzset 0 -1 WITHSCORES
                       a
                       1
                       b
                       2
                       c
                       3
               
  P.S.: USE QUIT to exit from the loop :)
