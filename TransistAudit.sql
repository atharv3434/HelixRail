-- Track timeline flow metrics of concurrent thread step updates
SELECT * FROM transit_ledger;

-- Aggregate occurrences where thread timeouts were triggered by long-running operations
SELECT train_id, COUNT(*) as timeout_incidents 
FROM transit_ledger 
WHERE action_type = 'LOCK_TIMEOUT' 
GROUP BY train_id;