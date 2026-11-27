import AVFoundation
import Foundation

/// Plays MMAPI audio handed off from Java `AudioBridge` (WAV via AVAudioPlayer, MIDI via AVMIDIPlayer + TimGM6mb).
///
/// Thread-safe: Java native callbacks may arrive off the main thread.
final class MMAAudioEngine: @unchecked Sendable {

    // MARK: - Nested

    private final class Slot {
        var wavPlayer: AVAudioPlayer?
        var midiPlayer: AVMIDIPlayer?
        var mime: String = ""
        var loopCount: Int = 1
        var volume: Float = 1
    }

    // MARK: - State

    private let lock = NSLock()
    private var slots: [Int32: Slot] = [:]
    private var sessionConfigured = false
    /// Bumped on every `stopAll` so in-flight MIDI loop threads exit.
    private var generation: UInt64 = 0

    // MARK: - Public

    /// Stops and drops every player. Called from Swift when a MIDlet session ends
    /// so leftover `AVMIDIPlayer` loops cannot keep the speaker busy.
    func stopAll() {
        lock.lock()
        generation &+= 1
        let all = Array(slots.values)
        slots.removeAll(keepingCapacity: true)
        lock.unlock()
        for slot in all {
            slot.wavPlayer?.stop()
            slot.midiPlayer?.stop()
        }
        print("corej2-audio: stopAll swift slots=\(all.count)")
    }

    func prepare(playerId: Int32, path: String, mime: String, volume: Int32, loopCount: Int32) {
        configureSessionIfNeeded()
        let url = URL(fileURLWithPath: path)
        guard FileManager.default.fileExists(atPath: path) else {
            print("corej2-audio: missing file \(path)")
            return
        }

        let slot = Slot()
        slot.mime = mime
        slot.loopCount = Int(loopCount)
        slot.volume = Self.normalizedVolume(volume)

        let lower = mime.lowercased()
        do {
            if lower.contains("mid") || path.lowercased().hasSuffix(".mid") {
                let bank = Self.soundBankURL()
                slot.midiPlayer = try AVMIDIPlayer(contentsOf: url, soundBankURL: bank)
                slot.midiPlayer?.prepareToPlay()
                print("corej2-audio: prepared MIDI id=\(playerId) bank=\(bank != nil)")
            } else {
                let player = try AVAudioPlayer(contentsOf: url)
                player.prepareToPlay()
                player.volume = slot.volume
                player.numberOfLoops = Self.avLoopCount(slot.loopCount)
                slot.wavPlayer = player
                print("corej2-audio: prepared WAV/other id=\(playerId)")
            }
            lock.lock()
            slots[playerId] = slot
            lock.unlock()
        } catch {
            print("corej2-audio: prepare failed id=\(playerId) \(error)")
        }
    }

    func start(playerId: Int32) {
        lock.lock()
        let slot = slots[playerId]
        lock.unlock()
        guard let slot else {
            print("corej2-audio: start missing id=\(playerId)")
            return
        }
        if let wav = slot.wavPlayer {
            wav.volume = slot.volume
            wav.numberOfLoops = Self.avLoopCount(slot.loopCount)
            wav.currentTime = 0
            wav.play()
            print("corej2-audio: start WAV id=\(playerId)")
            return
        }
        if let midi = slot.midiPlayer {
            midi.currentPosition = 0
            midi.play(nil)
            print("corej2-audio: start MIDI id=\(playerId)")
            // Simple loop for MIDI when requested forever / N times.
            if slot.loopCount != 0 && slot.loopCount != 1 {
                scheduleMidiLoop(playerId: playerId, remaining: slot.loopCount)
            }
        }
    }

    func stop(playerId: Int32) {
        lock.lock()
        let slot = slots[playerId]
        lock.unlock()
        slot?.wavPlayer?.stop()
        slot?.midiPlayer?.stop()
        print("corej2-audio: stop id=\(playerId)")
    }

    func close(playerId: Int32) {
        stop(playerId: playerId)
        lock.lock()
        slots.removeValue(forKey: playerId)
        lock.unlock()
        print("corej2-audio: close id=\(playerId)")
    }

    func setVolume(playerId: Int32, volume: Int32) {
        lock.lock()
        defer { lock.unlock() }
        guard let slot = slots[playerId] else { return }
        slot.volume = Self.normalizedVolume(volume)
        slot.wavPlayer?.volume = slot.volume
    }

    // MARK: - Private

    private func scheduleMidiLoop(playerId: Int32, remaining: Int) {
        // Best-effort: AVMIDIPlayer completion is async; poll duration.
        lock.lock()
        let duration = slots[playerId]?.midiPlayer?.duration ?? 0
        let startGeneration = generation
        lock.unlock()
        guard duration > 0 else { return }
        let forever = remaining < 0
        var left = remaining
        DispatchQueue.global(qos: .utility).async { [weak self] in
            while true {
                Thread.sleep(forTimeInterval: duration)
                guard let self else { return }
                self.lock.lock()
                let gen = self.generation
                let slot = self.slots[playerId]
                let player = slot?.midiPlayer
                self.lock.unlock()
                // Exit if stopAll ran or this player was closed.
                guard gen == startGeneration, let player, slot != nil else { return }
                if !forever {
                    left -= 1
                    if left <= 0 { return }
                }
                player.currentPosition = 0
                player.play(nil)
            }
        }
    }

    private func configureSessionIfNeeded() {
        lock.lock()
        let already = sessionConfigured
        lock.unlock()
        guard !already else { return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true)
            lock.lock()
            sessionConfigured = true
            lock.unlock()
        } catch {
            print("corej2-audio: session failed \(error)")
        }
    }

    private static func normalizedVolume(_ level: Int32) -> Float {
        max(0, min(1, Float(level) / 100))
    }

    private static func avLoopCount(_ midpLoops: Int) -> Int {
        if midpLoops < 0 { return -1 }
        if midpLoops <= 1 { return 0 }
        return midpLoops - 1
    }

    private static func soundBankURL() -> URL? {
        if let url = Bundle.main.url(forResource: "TimGM6mb", withExtension: "sf2") {
            return url
        }
        if let url = Bundle.main.url(
            forResource: "TimGM6mb",
            withExtension: "sf2",
            subdirectory: "Audio"
        ) {
            return url
        }
        return nil
    }
}
