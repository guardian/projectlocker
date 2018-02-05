import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import ErrorViewComponent from '../../../app/multistep/common/ErrorViewComponent.jsx';
import sinon from 'sinon';
import assert from 'assert';

describe("ErrorviewComponent", ()=>{
    it("should display an empty para if error object is null",()=>{
        const rendered = shallow(<ErrorViewComponent error={null}/>);
        expect(rendered.find('p.error-text').text()).toEqual("");
    });

    it("should direct user to the console log if there is a request but no response", ()=>{
        const fakeError ={
            request: {
                key: "value"
            }
        };

        const rendered = shallow(<ErrorViewComponent error={fakeError}/>);
        expect(rendered.find('p.error-text').text()).toEqual("No response from server. See console log for more details.");
    });

    it("should direct user to the console log if there is no request", ()=>{
        const fakeError = {
            message: "My hovercraft is full of eels"
        };

        const rendered = shallow(<ErrorViewComponent error={fakeError}/>);
        expect(rendered.find('p.error-text').text()).toEqual("Unable to set up request. See console log for more details.");
    });
});