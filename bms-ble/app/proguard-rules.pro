# Compose, AndroidX Lifecycle/ViewModel and Kotlin coroutines ship their own consumer
# rules inside their AARs (keeping ViewModel constructors, Compose runtime internals,
# etc.), so no custom keep rules are needed here yet.
#
# BMS protocol parsing (BmsFrameParser/BmsCommands/BmsStateReducer) is plain manual
# byte handling with no reflection, so it shrinks/obfuscates safely.
#
# If a future crash report shows a class R8 removed or renamed unexpectedly, add a
# targeted -keep rule here rather than disabling minification.
