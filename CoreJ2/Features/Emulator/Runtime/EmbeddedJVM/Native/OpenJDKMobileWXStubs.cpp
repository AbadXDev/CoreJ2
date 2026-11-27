// W^X / platform stubs required when linking the OpenJDK Mobile (Zero)
// static libraries into the iOS app image.
//
// The exact symbol set depends on the staged libjvm.a build; entries are
// added from linker "Undefined symbols" errors on device builds. The file
// must exist in the app target so the link step can pick the stubs up.
