import React from 'react';
import {shallow} from 'enzyme';
import ShowPasswordComponent from '../../app/multistep/ShowPasswordComponent.jsx';
import assert from 'assert';

describe("ShowPasswordComponent",()=>{
    it("should render a field which is not called password",()=>{
        const rendered=shallow(<ShowPasswordComponent fieldName="greenfield" pass="some value"/>);
        assert(rendered.find("span"));
        expect(rendered.find("span").hasClass("hidden-password")).toBeFalsy();
        expect(rendered.find("span").text()).toEqual("some value");
    });

    it("should obscure a field which is called password", ()=>{
        const rendered=shallow(<ShowPasswordComponent fieldName="password" pass="topsecret!donotreveal:)"/>);
        assert(rendered.find("span"));
        assert(rendered.find("span.hidden-password"));
        expect(rendered.find("span.hidden-password").text()).toEqual("***********************");
    });
});