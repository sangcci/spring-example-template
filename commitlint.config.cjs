module.exports = {
  extends: ["@commitlint/config-conventional"],
  parserPreset: {
    parserOpts: {
      headerPattern: /^(\w+): (.+)$/,
      headerCorrespondence: ["type", "subject"],
    },
  },
  rules: {
    "type-enum": [
      2,
      "always",
      ["feat", "fix", "docs", "style", "refactor", "test", "chore", "rename", "perf"],
    ],
    "subject-empty": [2, "never"],
    "subject-case": [0],
  },
};
