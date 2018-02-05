import React from 'react';
import {shallow,mount} from 'enzyme';
import moxios from 'moxios';
import StorageCompletionComponent from '../../../app/multistep/storage/CompletionComponent.jsx';
import sinon from 'sinon';
import assert from 'assert';

const strgTypes = [
    {
        name: 'type one'
    },
    {
        name: 'type two'
    }
];

const subfolder = "/subfolder";
const logindetails = {
    hostname: 'myhost.com',
    port: 8080,
    username: 'kevin',
    password: 'kaboom'
};

const currentstorage = {
    name: 'robert'
};

describe("StorageCompletionComponent", ()=>{
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    it("should display a summary of the options selected", ()=>{
        const rendered=shallow(<StorageCompletionComponent currentStorage={currentstorage}
                                                           strgTypes={strgTypes}
                                                           selectedType="1"
                                                           loginDetails={logindetails}
                                                           rootpath={subfolder}/>);

        const summary = rendered.find('SummaryComponent');
        expect(summary.props().name).toEqual('type two');
        expect(summary.props().loginDetails).toEqual(logindetails);
        expect(summary.props().subfolder).toEqual(subfolder);
    });

    it("should compile an object of data to send to the server", ()=>{
        const rendered=shallow(<StorageCompletionComponent currentStorage={currentstorage}
                                                           strgTypes={strgTypes}
                                                           selectedType="1"
                                                           loginDetails={logindetails}
                                                           rootpath={subfolder}/>);
        const rtn = rendered.instance().requestContent();
        expect(rtn).toEqual({
            rootpath: '/subfolder',
            storageType: 'type two',
            host: 'myhost.com',
            port: 8080,
            user: 'kevin',
            password: 'kaboom'
        });
    });

    it("should make a REST call to save data when Confirm is clicked then redirect to index", (done)=>{
        const rendered=shallow(<StorageCompletionComponent currentStorage={currentstorage}
                                                           strgTypes={strgTypes}
                                                           selectedType="1"
                                                           loginDetails={logindetails}
                                                           rootpath={subfolder}/>);
        const button = rendered.find('button');
        button.simulate('click');
        window.location.assign = sinon.spy();

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/api/storage');
            const request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {'status': 'ok'}
            }).then(() => {
                assert(window.location.assign.calledWith('/storage/'));
                done();
            }).catch(error=>{
                console.error(error);
                done(error);
            })
        });
    })
});