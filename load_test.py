import time
import json
import random
import threading
from paho.mqtt import client as mqtt_client

BROKER = 'localhost'
PORT = 1883
TOPICS = ['reading/waste-level']

# Test Parameters
NUM_DEVICES = 2000
PUB_FREQUENCY_SEC = 2    # 1000 devices / 1 sec = 1000 RPS
RUN_DURATION_SEC = 60
HARDWARE_ID_START = 11001  # HW-11001 to HW-12000

# Shared counters (thread-safe)
message_count = 0
failed_publish_count = 0
failed_connect_count = 0
counter_lock = threading.Lock()


class DeviceSimulator:
    def __init__(self, hardware_id, device_type):
        self.hardware_id = hardware_id
        self.device_type = device_type
        self.client = mqtt_client.Client(client_id=f"Sim-{hardware_id}")

    def connect(self):
        global failed_connect_count
        try:
            self.client.connect(BROKER, PORT)
            self.client.loop_start()
            return True
        except Exception as e:
            print(f"Failed to connect simulator {self.hardware_id}: {e}")
            with counter_lock:
                failed_connect_count += 1
            return False

    def publish_reading(self):
        global message_count, failed_publish_count
        v1 = round(random.uniform(10.0, 95.0), 2)
        v2 = round(random.uniform(5.0, 50.0), 2) if self.device_type == "DUAL_BIN" else None
        battery = round(random.uniform(60.0, 100.0), 2)

        payload = {
            "id": self.hardware_id,
            "v1": v1,
            "battery": battery
        }
        if v2 is not None:
            payload["v2"] = v2

        topic = random.choice(TOPICS)
        try:
            result = self.client.publish(topic, json.dumps(payload), qos=1)
            if result.rc == 0:
                with counter_lock:
                    message_count += 1
            else:
                with counter_lock:
                    failed_publish_count += 1
        except Exception:
            with counter_lock:
                failed_publish_count += 1

    def stop(self):
        self.client.loop_stop()
        self.client.disconnect()


def simulate_device(hardware_id, device_type, stop_event):
    sim = DeviceSimulator(hardware_id, device_type)
    if not sim.connect():
        return

    time.sleep(random.uniform(0, PUB_FREQUENCY_SEC))

    while not stop_event.is_set():
        sim.publish_reading()
        time.sleep(PUB_FREQUENCY_SEC)

    sim.stop()


if __name__ == '__main__':
    print(f"Starting simulation of {NUM_DEVICES} devices...")
    print(f"Hardware ID range: HW-{HARDWARE_ID_START} to HW-{HARDWARE_ID_START + NUM_DEVICES - 1}")
    print(f"Target rate: ~{NUM_DEVICES / PUB_FREQUENCY_SEC:.1f} messages/sec")

    stop_event = threading.Event()
    threads = []

    types = ["SINGLE_BIN", "DUAL_BIN"]
    hardware_ids = [f"HW-{i}" for i in range(HARDWARE_ID_START, HARDWARE_ID_START + NUM_DEVICES)]

    start_time = time.time()

    for hw_id in hardware_ids:
        dev_type = random.choice(types)
        t = threading.Thread(target=simulate_device, args=(hw_id, dev_type, stop_event))
        t.daemon = True
        t.start()
        threads.append(t)

    print(f"All {NUM_DEVICES} device threads started. Running load for {RUN_DURATION_SEC} seconds...\n")

    elapsed = 0
    interval = 10
    while elapsed < RUN_DURATION_SEC:
        sleep_time = min(interval, RUN_DURATION_SEC - elapsed)
        time.sleep(sleep_time)
        elapsed += sleep_time
        with counter_lock:
            current_count = message_count
            current_failed = failed_publish_count
        actual_rate = current_count / elapsed if elapsed > 0 else 0
        print(f"[{elapsed}s elapsed] published: {current_count} | "
              f"failed: {current_failed} | avg rate: {actual_rate:.1f} msg/sec")

    print("\nStopping simulation threads...")
    stop_event.set()
    for t in threads:
        t.join(timeout=1.0)

    total_time = time.time() - start_time

    print("\n===== SIMULATION SUMMARY =====")
    print(f"Devices simulated:      {NUM_DEVICES}")
    print(f"Hardware ID range:      HW-{HARDWARE_ID_START} to HW-{HARDWARE_ID_START + NUM_DEVICES - 1}")
    print(f"Total duration:         {total_time:.1f} sec")
    print(f"Failed connections:     {failed_connect_count}")
    print(f"Messages published:     {message_count}")
    print(f"Messages failed:        {failed_publish_count}")
    print(f"Actual avg rate:        {message_count / total_time:.1f} msg/sec")
    print("===============================")
    print("\nNext step: compare 'Messages published' against your Postgres row count:")
    print("  SELECT COUNT(*) FROM sensor_reading WHERE recorded_at > '<test start timestamp>';")