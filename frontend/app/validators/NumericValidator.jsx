const intValidator = new RegExp('^\\d+$');

export function validateInt(input){
    const result=intValidator.test(input);
    console.log("validation result for '"+ input, "': ", result);
    return result ? null : "This must be an integer"
}