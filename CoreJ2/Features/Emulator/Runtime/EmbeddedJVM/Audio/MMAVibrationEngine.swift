import Foundation
import UIKit

/// Plays Core Haptics / UIKit vibration for MIDP `Display.vibrate` and OEM APIs.
final class MMAVibrationEngine: @unchecked Sendable {

    private let lock = NSLock()
    private var stopWorkItem: DispatchWorkItem?

    /// Vibrates for `durationMs` milliseconds. `0` cancels an in-flight pulse.
    func vibrate(durationMs: Int32) {
        let enabled = UserDefaults.standard.object(forKey: "corej2.settings.vibrationEnabled") as? Bool ?? true
        guard enabled else {
            print("corej2-vibrate: skipped (settings off)")
            return
        }
        lock.lock()
        stopWorkItem?.cancel()
        stopWorkItem = nil
        lock.unlock()

        guard durationMs > 0 else {
            print("corej2-vibrate: cancel")
            return
        }

        let duration = TimeInterval(durationMs) / 1000.0
        DispatchQueue.main.async {
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.prepare()
            generator.impactOccurred(intensity: 1.0)
            // Repeat soft pulses to approximate long MIDP vibrate durations.
            let pulses = max(1, Int(duration / 0.12))
            var remaining = pulses - 1
            let work = DispatchWorkItem { [weak self] in
                guard remaining > 0 else { return }
                remaining -= 1
                generator.impactOccurred(intensity: 0.85)
                if remaining > 0 {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.12, execute: DispatchWorkItem {
                        self?.continuePulse(generator: generator, remaining: remaining)
                    })
                }
            }
            self.lock.lock()
            self.stopWorkItem = work
            self.lock.unlock()
            if remaining > 0 {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.12, execute: work)
            }
            print("corej2-vibrate: start \(durationMs)ms pulses=\(pulses)")
        }
    }

    private func continuePulse(generator: UIImpactFeedbackGenerator, remaining: Int) {
        lock.lock()
        let cancelled = stopWorkItem == nil
        lock.unlock()
        guard !cancelled, remaining > 0 else { return }
        generator.impactOccurred(intensity: 0.85)
        if remaining > 1 {
            let next = remaining - 1
            let work = DispatchWorkItem { [weak self] in
                self?.continuePulse(generator: generator, remaining: next)
            }
            lock.lock()
            stopWorkItem = work
            lock.unlock()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.12, execute: work)
        }
    }
}
