import SwiftUI

/// Lets the user pick the device profile (LCD size) used to run a game.
///
/// Profiles are grouped by handset brand. The game's current profile is
/// marked with a checkmark.
struct DeviceProfilePickerView: View {

    // MARK: - Dependencies

    private let game: InstalledGame
    private let onSelect: (DeviceProfile) -> Void

    @Environment(\.dismiss) private var dismiss

    // MARK: - Init

    init(game: InstalledGame, onSelect: @escaping (DeviceProfile) -> Void) {
        self.game = game
        self.onSelect = onSelect
    }

    // MARK: - Body

    var body: some View {
        NavigationStack {
            List {
                ForEach(DeviceProfile.profilesByBrand, id: \.brand) { group in
                    Section(group.brand) {
                        ForEach(group.profiles) { profile in
                            profileRow(profile)
                        }
                    }
                }
            }
            .navigationTitle("Device Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    // MARK: - Rows

    private func profileRow(_ profile: DeviceProfile) -> some View {
        Button {
            onSelect(profile)
            dismiss()
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(profile.name)
                        .foregroundStyle(.primary)

                    Text(profile.resolutionLabel)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                if profile.id == game.deviceProfileID {
                    Image(systemName: "checkmark")
                        .foregroundStyle(.tint)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
