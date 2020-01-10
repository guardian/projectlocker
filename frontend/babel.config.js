module.exports = {
    "presets": [
        "@babel/env",
        "@babel/react"
    ],
    "plugins": [
        "@babel/plugin-proposal-function-bind",
        "@babel/plugin-proposal-class-properties",
        "@babel/plugin-transform-react-jsx",
        //plugin-transform-runtime is required for testing and using the `async/await` construct, but has been giving me
        //hell by causing strange TypeErrors on module imports when running in jest.
        //specifying the extra options here seems to fix it -https://babeljs.io/docs/en/babel-plugin-transform-runtime
        [ "@babel/plugin-transform-runtime",{
            "absoluteRuntime": false,
            "corejs": false,
            "helpers": false,
            "regenerator": true,
            "useESModules": false,
            "version": "7.7.6"
        }]
    ]
};
