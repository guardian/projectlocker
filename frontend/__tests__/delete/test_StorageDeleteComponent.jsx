import React from 'react';
import {shallow,mount} from 'enzyme';
import StorageDeleteComponent from '../../app/delete/StorageDeleteComponent.jsx';
import assert from 'assert';
import moxios from 'moxios';

describe("StorageDeleteComponent", ()=>{
    beforeEach(() => moxios.install());
    afterEach(() => moxios.uninstall());

    it("should return an appropriate description",(done)=>{
        const rendered = shallow(<StorageDeleteComponent match={{params: {itemid: 9}}}/>);

        return moxios.wait(()=>{
            expect(moxios.requests.mostRecent().config.url).toEqual('/api/storage/9');
            let request = moxios.requests.mostRecent();
            request.respondWith({
                status: 200,
                response: {'status': 'ok','result': {
                    storageType: 'something', rootpath: '/path/to/storage', user: "fred"
                }}
            }).then(() => {
                rendered.update();

                /* SummaryComponent itself has its own unit test */
                const component = rendered.find("SummaryComponent");
                assert(component);
                expect(component.props().name).toEqual("something");
                expect(component.props().subfolder).toEqual("/path/to/storage");
                expect(component.props().loginDetails.user).toEqual("fred");
                expect(component.props().loginDetails.host).toEqual(undefined);
                expect(component.props().loginDetails.port).toEqual(undefined);
                expect(component.props().loginDetails.password).toEqual(undefined);
                done();
            }).catch(error=>{
                console.error(error);
                done.fail(error);
            })
        });

    });
});