import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import SummaryComponent from '../../../app/multistep/storage/SummaryComponent.jsx';
import sinon from 'sinon';

const entries = require("object.entries");

describe("Storage SummaryComponent",()=>{
    it("should render a summary based on the props given", ()=>{
        if(!Object.entries) entries.shim();

        const rendered = shallow(<SummaryComponent
            name="StorageType"
            loginDetails={{host: "somehostname",port: "1234",user: "someusername",password: "somepassword"}}
            subfolder="/path/to/root"
        />);

        expect(rendered.find('table')).toBeTruthy();
        expect(rendered.find('td#storageType').text()).toEqual("StorageType");
        expect(rendered.find('.login-description').length).toEqual(4);
        expect(rendered.find('.login-value').length).toEqual(4);

        expect(rendered.find('.login-description').at(0).text()).toEqual("host: ");
        expect(rendered.find('.login-description').at(1).text()).toEqual("port: ");
        expect(rendered.find('.login-description').at(2).text()).toEqual("user: ");
        expect(rendered.find('.login-description').at(3).text()).toEqual("password: ");

        expect(rendered.find('.login-value').at(0).html()).toEqual("<span class=\"login-value\"><span>somehostname</span></span>");
        expect(rendered.find('.login-value').at(1).html()).toEqual("<span class=\"login-value\"><span>1234</span></span>");
        expect(rendered.find('.login-value').at(2).html()).toEqual("<span class=\"login-value\"><span>someusername</span></span>");
        expect(rendered.find('.login-value').at(3).html()).toEqual("<span class=\"login-value\"><span class=\"hidden-password\">************</span></span>");
    })
});