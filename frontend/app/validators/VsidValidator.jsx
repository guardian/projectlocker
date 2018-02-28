const vsidValidator = new RegExp('\\w{2}-\\d+');

export function validateVsid(input){
    const result=vsidValidator.test(input);
    console.log("validation result for '"+ input, "': ", result);
    return result ? null : "This must be in the form of XX-nnnnn"
}