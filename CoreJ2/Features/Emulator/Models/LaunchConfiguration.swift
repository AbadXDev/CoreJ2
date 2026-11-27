import Foundation

/// Inputs required to launch an installed Java ME game through the emulator bridge.
struct LaunchConfiguration: Equatable, Sendable, Hashable {

    // MARK: - Properties

    /// The installed game that should be launched.
    let game: InstalledGame

    /// LCD width the emulator platform is created with.
    let lcdWidth: Int

    /// LCD height the emulator platform is created with.
    let lcdHeight: Int

    // MARK: - Init

    /// Creates a configuration using the game's device profile LCD size.
    init(game: InstalledGame) {
        self.game = game
        self.lcdWidth = game.deviceProfile.lcdWidth
        self.lcdHeight = game.deviceProfile.lcdHeight
    }

    /// Creates a configuration with an explicit LCD size.
    init(game: InstalledGame, lcdWidth: Int, lcdHeight: Int) {
        self.game = game
        self.lcdWidth = lcdWidth
        self.lcdHeight = lcdHeight
    }
}
