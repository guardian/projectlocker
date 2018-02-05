import React from 'react';

class StorageTypeComponent extends React.Component {
    constructor(props){
        super(props);

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }

    selectorValueChanged(event){
        this.props.valueWasSet(parseInt(event.target.value));
    }

    render() {
        console.log(this.props.strgTypes);
        return(<div>
            <h3>Storage Type</h3>
            <p className="information">The first piece of information we need is what kind of storage to connect to.
                Different storages require different configuration options; currently we support a local disk, ObjectMatrix vault,
                or an S3 bucket.
            </p>
            <select id="storage_type_selector" value={this.props.selectedType} onChange={this.selectorValueChanged}>
                {
                    this.props.strgTypes.map((typeInfo, index)=><option key={index} value={index}>{typeInfo.name}</option>)
                }
            </select>
        </div>
        )

    }
}

export default StorageTypeComponent;
