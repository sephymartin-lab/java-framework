// https://github.com/lint-staged/lint-staged?tab=readme-ov-file#using-js-configuration-files
// lint-staged.config.js
// import micromatch from 'micromatch'

const fs = require('fs')

/**
 * 过滤掉软链接文件
 * @param {string[]} filenames 文件路径数组
 * @returns {string[]} 过滤后的文件路径数组
 */
function filterSymlinks(filenames) {
  return filenames.filter((filename) => {
    try {
      // 使用 lstatSync 而不是 statSync，因为 lstat 不会跟随软链接
      const stats = fs.lstatSync(filename)
      // 如果是软链接，返回 false（过滤掉）
      return !stats.isSymbolicLink()
    } catch (error) {
      // 如果文件不存在或无法访问，也过滤掉
      return false
    }
  })
}

// https://www.cnblogs.com/jiaoshou/p/12250278.html
module.exports = {
  'framework-dependencies/**/*.{java,kt}|**/pom.xml': (filenames) => {
    const filteredFiles = filterSymlinks(filenames)
    if (filteredFiles.length === 0) {
      return '' // 如果没有文件需要处理，返回空字符串跳过
    }
    return `cd framework-dependencies && ./mvnw spotless:apply -DspotlessFiles=${filteredFiles.join(
      ','
    )}`
  },
  'framework-infra/**/*.{java,kt}|**/pom.xml': (filenames) => {
    const filteredFiles = filterSymlinks(filenames)
    if (filteredFiles.length === 0) {
      return '' // 如果没有文件需要处理，返回空字符串跳过
    }
    return `cd framework-infra && ./mvnw spotless:apply -DspotlessFiles=${filteredFiles.join(
      ','
    )}`
  },
  '**/!(pnpm-lock).{json,js,yml,yaml}': (filenames) => {
    const filteredFiles = filterSymlinks(filenames)
    if (filteredFiles.length === 0) {
      return '' // 如果没有文件需要处理，返回空字符串跳过
    }
    return `prettier --write --ignore-unknown ${filteredFiles.join(' ')}`
  },
}
