import concurrent.futures
import time
import random

def mock_train_lifecycle(train_index: int):
    """
    Simulates a distinct thread entity running execution loops to contend for global lock states.
    """
    train_id = f"TRN-EXPRESS-{train_index}"
    sleep_delay = random.uniform(0.1, 0.8)
    
    # Simulating structural delay cycles before lock execution requests
    time.sleep(sleep_delay)
    
    processing_compute_ms = random.randint(200, 600)
    return {
        "train": train_id,
        "action_required": "LOCK_ACQUIRE",
        "simulated_track_hold_ms": processing_compute_ms
    }

if __name__ == "__main__":
    print("Pre-compiling concurrent execution matrix streams...")
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        futures = [executor.submit(mock_train_lifecycle, i) for i in range(1, 6)]
        for f in concurrent.futures.as_completed(futures):
            res = f.result()
            print(f"Simulation Vector ── {res['train']} ready to fire lock request. Planned occupation: {res['simulated_track_hold_ms']}ms")