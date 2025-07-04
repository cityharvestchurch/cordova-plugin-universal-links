/*
Script activates support for Universal Links in the application by setting proper preferences in the xcode project file.
Which is:
- deployment target set to iOS 9.0
- .entitlements file added to project PBXGroup and PBXFileReferences section
- path to .entitlements file added to Code Sign Entitlements preference
*/

var path = require('path');
var compare = require('node-version-compare');
var ConfigXmlHelper = require('../configXmlHelper.js');
var IOS_DEPLOYMENT_TARGET = '9.0';
var COMMENT_KEY = /_comment$/;
var context;

module.exports = {
  enableAssociativeDomainsCapability: enableAssociativeDomainsCapability
}

// region Public API

/**
 * Activate associated domains capability for the application.
 *
 * @param {Object} cordovaContext - cordova context object
 */
function enableAssociativeDomainsCapability(cordovaContext) {
  context = cordovaContext;

  var projectFile = loadProjectFile();

  // adjust preferences
  //activateAssociativeDomains(projectFile.xcode);

  // add entitlements file to pbxfilereference
  addPbxReference(projectFile.xcode);

  // save changes
  //projectFile.write();
}

// endregion

// region Alter project file preferences

/**
 * Activate associated domains support in the xcode project file:
 * - set deployment target to ios 9;
 * - add .entitlements file to Code Sign Entitlements preference.
 *
 * @param {Object} xcodeProject - xcode project preferences; all changes are made in that instance
 */
function activateAssociativeDomains(xcodeProject) {
  var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection());
  var entitlementsFilePath = pathToEntitlementsFile();
  var config;
  var buildSettings;
  var deploymentTargetIsUpdated;

  for (config in configurations) {
    buildSettings = configurations[config].buildSettings;console.log(buildSettings);
    buildSettings['CODE_SIGN_ENTITLEMENTS'] = '"' + entitlementsFilePath + '"';

    // if deployment target is less then the required one - increase it
    if (buildSettings['IPHONEOS_DEPLOYMENT_TARGET']) {
      if (compare(buildSettings['IPHONEOS_DEPLOYMENT_TARGET'], IOS_DEPLOYMENT_TARGET) == -1) {
        buildSettings['IPHONEOS_DEPLOYMENT_TARGET'] = IOS_DEPLOYMENT_TARGET;
        deploymentTargetIsUpdated = true;
      }
    } else {
      buildSettings['IPHONEOS_DEPLOYMENT_TARGET'] = IOS_DEPLOYMENT_TARGET;
      deploymentTargetIsUpdated = true;
    }
  }

  if (deploymentTargetIsUpdated) {
    console.log('IOS project now has deployment target set as: ' + IOS_DEPLOYMENT_TARGET);
  }

  console.log('IOS project Code Sign Entitlements now set to: ' + entitlementsFilePath);
}

// endregion

// region PBXReference methods

/**
 * Add .entitlemets file into the project.
 *
 * @param {Object} xcodeProject - xcode project preferences; all changes are made in that instance
 */
function addPbxReference(xcodeProject) {
  var fileReferenceSection = nonComments(xcodeProject.pbxFileReferenceSection());
  var entitlementsFileName = path.basename(pathToEntitlementsFile());

  if (isPbxReferenceAlreadySet(fileReferenceSection, entitlementsFileName)) {
    console.log('Entitlements file is in reference section.');
    return;
  }

  console.log('Entitlements file is not in references section, adding it');
  xcodeProject.addResourceFile(entitlementsFileName);
}

/**
 * Check if .entitlemets file reference already set.
 *
 * @param {Object} fileReferenceSection - PBXFileReference section
 * @param {String} entitlementsRelativeFilePath - relative path to entitlements file
 * @return true - if reference is set; otherwise - false
 */
function isPbxReferenceAlreadySet(fileReferenceSection, entitlementsRelativeFilePath) {
  var isAlreadyInReferencesSection = false;
  var uuid;
  var fileRefEntry;

  for (uuid in fileReferenceSection) {
    fileRefEntry = fileReferenceSection[uuid];
    if (fileRefEntry.path && fileRefEntry.path.indexOf(entitlementsRelativeFilePath) > -1) {
      isAlreadyInReferencesSection = true;
      break;
    }
  }

  return isAlreadyInReferencesSection;
}

// region Xcode project file helpers

/**
 * Load iOS project file from platform specific folder.
 *
 * @return {Object} projectFile - project file information
 */
function loadProjectFile() {
    var platform_ios;
    var projectFile;
    try {
        // try pre-5.0 cordova structure
        platform_ios = context.requireCordovaModule('cordova-lib/src/plugman/platforms')['ios'];
        projectFile = platform_ios.parseProjectFile(iosPlatformPath());
    } catch (e) {console.log(e);
        try {
            // let's try cordova 5.0 structure
            platform_ios = context.requireCordovaModule('cordova-lib/src/plugman/platforms/ios');
            projectFile = platform_ios.parse(iosPlatformPath());
        } catch (e) {console.log(e);
            // try cordova 7.0 structure
            try{
                var iosPlatformApi = require(path.join(iosPlatformPath(), '/cordova/Api'));console.log(iosPlatformApi)
                //var projectFileApi = require(path.join(iosPlatformPath(), '/cordova/lib/projectFile.js'));
                //var parse = require(path.join(iosPlatformPath(), '/cordova/parseProjectFile'));
                var locations = (new iosPlatformApi('ios', iosPlatformPath())).locations;console.log(locations);
                //projectFile = projectFileApi.parse(locations);
                //projectFile = parse(locations);console.log(projectFile);

                projectFile = parseProjectFile(locations);console.log(projectFile);
            } catch(e){
                console.log(e);
            }
            
        }
    }
    return projectFile;
}

/**
 * Remove comments from the file.
 *
 * @param {Object} obj - file object
 * @return {Object} file object without comments
 */
function nonComments(obj) {
  var keys = Object.keys(obj);
  var newObj = {};

  for (var i = 0, len = keys.length; i < len; i++) {
    if (!COMMENT_KEY.test(keys[i])) {
      newObj[keys[i]] = obj[keys[i]];
    }
  }

  return newObj;
}

// endregion

// region Path helpers

function iosPlatformPath() {
  return path.join(projectRoot(), 'platforms', 'ios');
}

function projectRoot() {
  return context.opts.projectRoot;
}

function pathToEntitlementsFile() {
  var configXmlHelper = new ConfigXmlHelper(context),
    projectName = configXmlHelper.getProjectName(),
    fileName = projectName + '.entitlements';

  return path.join(projectName, 'Resources', fileName);
}

// endregion

const cachedProjectFiles = {};
const xcode = require('xcode');
const fs = require('fs');

function parseProjectFile (locations) {
    const project_dir = locations.root;
    const pbxPath = locations.pbxproj;

    if (cachedProjectFiles[project_dir]) {
        return cachedProjectFiles[project_dir];
    }

    const xcodeproj = xcode.project(pbxPath);
    xcodeproj.parseSync();

    //const config_file = path.join(project_dir, 'App', 'config.xml');
    var configXmlHelper = new ConfigXmlHelper(context);
    const config_file = configXmlHelper.getConfigXmlFilePath();

    if (!fs.existsSync(config_file)) {
        throw new CordovaError('Could not find config.xml file.');
    }

    const frameworks_file = path.join(project_dir, 'frameworks.json');
    let frameworks = {};
    try {
        frameworks = require(frameworks_file);
    } catch (e) { }

    const xcode_dir = path.join(project_dir, 'App');
    const pluginsDir = path.resolve(xcode_dir, 'Plugins');
    const resourcesDir = path.resolve(xcode_dir, 'Resources');

    cachedProjectFiles[project_dir] = {
        plugins_dir: pluginsDir,
        resources_dir: resourcesDir,
        xcode: xcodeproj,
        xcode_path: xcode_dir,
        pbx: pbxPath,
        projectDir: project_dir,
        platformWww: path.join(project_dir, 'platform_www'),
        www: path.join(project_dir, 'www'),
        write: function () {
            fs.writeFileSync(pbxPath, xcodeproj.writeSync({ omitEmptyValues: true }));
            if (Object.keys(this.frameworks).length === 0) {
                // If there is no framework references remain in the project, just remove this file
                fs.rmSync(frameworks_file, { force: true });
                return;
            }
            fs.writeFileSync(frameworks_file, JSON.stringify(this.frameworks, null, 4));
        },
        getPackageName: function () {
            return xcodeproj.getBuildProperty('PRODUCT_BUNDLE_IDENTIFIER', undefined, 'App').replace(/^"/, '').replace(/"$/, '');
        },
        getInstaller: function (name) {
            return pluginHandlers.getInstaller(name);
        },
        getUninstaller: function (name) {
            return pluginHandlers.getUninstaller(name);
        },
        frameworks
    };
    return cachedProjectFiles[project_dir];
}