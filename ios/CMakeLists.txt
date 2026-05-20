cmake_minimum_required(VERSION 3.20)

set(CMAKE_SYSTEM_NAME iOS)
set(CMAKE_OSX_DEPLOYMENT_TARGET 16.0)
set(CMAKE_Swift_LANGUAGE_VERSION 5.0)

project(sumahohikakuku LANGUAGES Swift OBJC)

set(IOS_DEVELOPMENT_TEAM "77V64TWCFD" CACHE STRING "Apple Developer Team ID for signing App Store builds")

# Set Bundle properties
set(MACOSX_BUNDLE_GUI_IDENTIFIER "com.porarrirr.sumahohikakuku")
set(MACOSX_BUNDLE_BUNDLE_NAME "sumahohikakuku")
set(MACOSX_BUNDLE_BUNDLE_VERSION "1.0.0")
set(MACOSX_BUNDLE_SHORT_VERSION_STRING "1.0")

# Collect Swift source files
file(GLOB_RECURSE SOURCES
    "src/*.swift"
)

# Reference CSV resource
set(RESOURCES
    "resources/sensor_database.csv"
    "resources/PrivacyInfo.xcprivacy"
    "resources/Assets.xcassets"
)

# App target
add_executable(sumahohikakuku MACOSX_BUNDLE
    ${SOURCES}
    ${RESOURCES}
)

# Copy resource files to app bundle
set_source_files_properties(${RESOURCES} PROPERTIES
    MACOSX_PACKAGE_LOCATION Resources
)

# Set target properties for iOS app
set_target_properties(sumahohikakuku PROPERTIES
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist"
    XCODE_ATTRIBUTE_PRODUCT_BUNDLE_IDENTIFIER "com.porarrirr.sumahohikakuku"
    XCODE_ATTRIBUTE_SUPPORTED_PLATFORMS "iphonesimulator iphoneos"
    XCODE_ATTRIBUTE_TARGETED_DEVICE_FAMILY "1" # iPhone
    XCODE_ATTRIBUTE_IPHONEOS_DEPLOYMENT_TARGET "16.0"
    XCODE_ATTRIBUTE_CODE_SIGN_STYLE "Automatic"
    XCODE_ATTRIBUTE_ASSETCATALOG_COMPILER_APPICON_NAME "AppIcon"
    XCODE_ATTRIBUTE_SKIP_INSTALL "NO"
    XCODE_ATTRIBUTE_INSTALL_PATH "$(LOCAL_APPS_DIR)"
    XCODE_ATTRIBUTE_INFOPLIST_KEY_CFBundleDisplayName "スマホセンサー比較"
    XCODE_ATTRIBUTE_INFOPLIST_KEY_CFBundleIconName "AppIcon"
    XCODE_ATTRIBUTE_INFOPLIST_KEY_ITSAppUsesNonExemptEncryption "NO"
    XCODE_ATTRIBUTE_INFOPLIST_KEY_NSPhotoLibraryAddUsageDescription "比較結果の画像を写真ライブラリに保存するために使用します。"
    XCODE_ATTRIBUTE_INFOPLIST_KEY_UISupportedInterfaceOrientations "UIInterfaceOrientationPortrait"
)

if (IOS_DEVELOPMENT_TEAM)
    set_target_properties(sumahohikakuku PROPERTIES
        XCODE_ATTRIBUTE_DEVELOPMENT_TEAM "${IOS_DEVELOPMENT_TEAM}"
    )
endif()

# Collect Test files
file(GLOB TEST_SOURCES
    "tests/*.swift"
)

if (TEST_SOURCES)
    # Create test bundle (xctest)
    add_library(sumahohikakukuTests MODULE ${TEST_SOURCES})
    
    # Configure it as an XCTest bundle linked with the app
    set_target_properties(sumahohikakukuTests PROPERTIES
        XCODE_PRODUCT_TYPE "com.apple.product-type.bundle.unit-test"
        BUNDLE TRUE
        BUNDLE_EXTENSION xctest
        XCODE_ATTRIBUTE_WRAPPER_EXTENSION xctest
        XCODE_ATTRIBUTE_BUNDLE_LOADER "$(TEST_HOST)"
        XCODE_ATTRIBUTE_TEST_HOST "$(BUILT_PRODUCTS_DIR)/sumahohikakuku.app/sumahohikakuku"
        XCODE_ATTRIBUTE_IPHONEOS_DEPLOYMENT_TARGET "16.0"
        XCODE_ATTRIBUTE_TARGETED_DEVICE_FAMILY "1"
        XCODE_ATTRIBUTE_SUPPORTED_PLATFORMS "iphonesimulator iphoneos"
        XCODE_ATTRIBUTE_FRAMEWORK_SEARCH_PATHS "$(PLATFORM_DIR)/Developer/Library/Frameworks"
    )
    
    target_link_libraries(sumahohikakukuTests PRIVATE "-framework XCTest")
    add_dependencies(sumahohikakukuTests sumahohikakuku)
endif()
